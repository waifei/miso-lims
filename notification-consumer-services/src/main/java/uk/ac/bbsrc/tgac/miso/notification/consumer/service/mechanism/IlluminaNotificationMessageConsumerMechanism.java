/*
 * Copyright (c) 2012. The Genome Analysis Centre, Norwich, UK
 * MISO project contacts: Robert Davey, Mario Caccamo @ TGAC
 * *********************************************************************
 *
 * This file is part of MISO.
 *
 * MISO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MISO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MISO.  If not, see <http://www.gnu.org/licenses/>.
 *
 * *********************************************************************
 */

package uk.ac.bbsrc.tgac.miso.notification.consumer.service.mechanism;

import static uk.ac.bbsrc.tgac.miso.core.util.LimsUtils.isStringEmptyOrNull;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.util.Assert;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import uk.ac.bbsrc.tgac.miso.core.data.Partition;
import uk.ac.bbsrc.tgac.miso.core.data.Run;
import uk.ac.bbsrc.tgac.miso.core.data.SequencerPartitionContainer;
import uk.ac.bbsrc.tgac.miso.core.data.SequencerPoolPartition;
import uk.ac.bbsrc.tgac.miso.core.data.SequencerReference;
import uk.ac.bbsrc.tgac.miso.core.data.SequencingParameters;
import uk.ac.bbsrc.tgac.miso.core.data.Status;
import uk.ac.bbsrc.tgac.miso.core.data.impl.PartitionImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.SequencerPartitionContainerImpl;
import uk.ac.bbsrc.tgac.miso.core.data.impl.illumina.IlluminaRun;
import uk.ac.bbsrc.tgac.miso.core.data.impl.illumina.IlluminaStatus;
import uk.ac.bbsrc.tgac.miso.core.data.type.HealthType;
import uk.ac.bbsrc.tgac.miso.core.data.type.PlatformType;
import uk.ac.bbsrc.tgac.miso.core.exception.InterrogationException;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;
import uk.ac.bbsrc.tgac.miso.core.service.SequencingParametersCollection;
import uk.ac.bbsrc.tgac.miso.core.service.integration.mechanism.NotificationMessageConsumerMechanism;
import uk.ac.bbsrc.tgac.miso.notification.service.IlluminaTransformer;
import uk.ac.bbsrc.tgac.miso.tools.run.RunFolderConstants;

/**
 * uk.ac.bbsrc.tgac.miso.core.service.integration.mechanism.impl
 * <p/>
 * Info
 * 
 * @author Rob Davey
 * @date 03/02/12
 * @since 0.1.5
 */
public class IlluminaNotificationMessageConsumerMechanism
    implements NotificationMessageConsumerMechanism<Message<Map<String, List<String>>>, Set<Run>> {
  protected static final Logger log = LoggerFactory.getLogger(IlluminaNotificationMessageConsumerMechanism.class);

  public boolean attemptRunPopulation = true;

  public void setAttemptRunPopulation(boolean attemptRunPopulation) {
    this.attemptRunPopulation = attemptRunPopulation;
  }

  private final String runDirRegex = RunFolderConstants.ILLUMINA_FOLDER_NAME_GROUP_CAPTURE_REGEX;
  private final Pattern p = Pattern.compile(runDirRegex);
  private final DateFormat logDateFormat = new SimpleDateFormat("MM'/'dd'/'yyyy','HH:mm:ss");
  private final DateFormat anotherLogDateFormat = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH:mm:ss");
  private final DateFormat illuminaRunFolderDateFormat = new SimpleDateFormat("yyMMdd");
  @Autowired
  private SequencingParametersCollection parameterSet;

  @Override
  public Set<Run> consume(Message<Map<String, List<String>>> message) throws InterrogationException {
    RequestManager requestManager = message.getHeaders().get("handler", RequestManager.class);
    Assert.notNull(requestManager, "Cannot consume MISO notification messages without a RequestManager.");
    Map<String, List<String>> statuses = message.getPayload();
    Set<Run> output = new HashSet<>();
    for (String key : statuses.keySet()) {
      HealthType ht = HealthType.valueOf(key);
      JSONArray runs = (JSONArray) JSONArray.fromObject(statuses.get(key)).get(0);
      Map<String, Run> map = processRunJSON(ht, runs, requestManager);
      for (Run r : map.values()) {
        output.add(r);
      }
    }
    return output;
  }

  private Map<String, Run> processRunJSON(HealthType ht, JSONArray runs, RequestManager requestManager) {
    Map<String, Run> updatedRuns = new HashMap<>();
    List<Run> runsToSave = new ArrayList<>();
    StringBuilder sb = new StringBuilder();

    for (JSONObject run : (Iterable<JSONObject>) runs) {
      String runName = run.getString(IlluminaTransformer.JSON_RUN_NAME);
      sb.append("Processing " + runName);
      log.debug("Processing " + runName);
      Status is = new IlluminaStatus();
      is.setRunName(runName);
      Run r = null;

      Matcher m = p.matcher(runName);
      if (m.matches()) {
        try {
          r = requestManager.getRunByAlias(runName);
        } catch (IOException ioe) {
          log.warn("Cannot find run by the alias " + runName
              + ". This usually means the run hasn't been previously imported. If attemptRunPopulation is false, processing will not take place for this run!");
        }
      }

      try {
        if (attemptRunPopulation) {
          if (r == null) {
            log.debug("Saving new run and status: " + runName);
            if (!run.has(IlluminaTransformer.JSON_STATUS)) {
              // probably MiSeq
              r = new IlluminaRun();
              r.setPlatformRunId(Integer.parseInt(m.group(2)));
              r.setAlias(runName);
              r.setFilePath(runName);
              r.setDescription(m.group(3));
              r.setPairedEnd(false);
              is.setHealth(ht);
              r.setStatus(is);
            } else {
              String xml = run.getString(IlluminaTransformer.JSON_STATUS);
              is = new IlluminaStatus(xml);
              r = new IlluminaRun(xml);
              is.setHealth(ht);
              r.getStatus().setHealth(ht);
            }

            if (run.has(IlluminaTransformer.JSON_FULL_PATH)) {
              r.setFilePath(run.getString(IlluminaTransformer.JSON_FULL_PATH));
            }

            if (run.has(IlluminaTransformer.JSON_NUM_CYCLES)) {
              r.setCycles(Integer.parseInt(run.getString(IlluminaTransformer.JSON_NUM_CYCLES)));
            }

            SequencerReference sr = null;
            if (run.has(IlluminaTransformer.JSON_SEQUENCER_NAME)) {
              sr = requestManager.getSequencerReferenceByName(run.getString(IlluminaTransformer.JSON_SEQUENCER_NAME));
              r.getStatus().setInstrumentName(run.getString(IlluminaTransformer.JSON_SEQUENCER_NAME));
              r.setSequencerReference(sr);
            }
            if (r.getSequencerReference() == null) {
              sr = requestManager.getSequencerReferenceByName(m.group(1));
              r.setSequencerReference(sr);
            }
            if (r.getSequencerReference() == null) {
              sr = requestManager.getSequencerReferenceByName(r.getStatus().getInstrumentName());
              r.setSequencerReference(sr);
            }

            if (r.getSequencerReference() == null) {
              log.error("Cannot save " + is.getRunName() + ": no sequencer reference available.");
            } else {
              log.debug("Setting sequencer reference: " + sr.getName());

              if (run.has(IlluminaTransformer.JSON_START_DATE)) {
                try {
                  if (!isStringEmptyOrNull(run.getString(IlluminaTransformer.JSON_START_DATE)) && !"null".equals(run.getString(IlluminaTransformer.JSON_START_DATE))) {
                    log.debug("Updating start date:" + run.getString(IlluminaTransformer.JSON_START_DATE));
                    r.getStatus().setStartDate(illuminaRunFolderDateFormat.parse(run.getString(IlluminaTransformer.JSON_START_DATE)));
                  }
                } catch (ParseException e) {
                  log.error("run JSON", e);
                }
              }

              if (run.has(IlluminaTransformer.JSON_COMPLETE_DATE)) {
                try {
                  if (!"null".equals(run.getString(IlluminaTransformer.JSON_COMPLETE_DATE)) && !isStringEmptyOrNull(run.getString(IlluminaTransformer.JSON_COMPLETE_DATE))) {
                    log.debug("Updating completion date:" + run.getString(IlluminaTransformer.JSON_COMPLETE_DATE));
                    r.getStatus().setCompletionDate(logDateFormat.parse(run.getString(IlluminaTransformer.JSON_COMPLETE_DATE)));
                  }
                } catch (ParseException e) {
                  log.error("run JSON", e);
                }
              }

              processRunParams(run, r);
            }
          } else {
            log.debug("Updating existing run and status: " + runName);

            // always overwrite any previous alias with the correct run alias
            r.setAlias(runName);
            r.setPlatformType(PlatformType.ILLUMINA);

            // update description if empty
            if (isStringEmptyOrNull(r.getDescription())) {
              r.setDescription(m.group(3));
            }

            if (r.getStatus() != null) {
              if (!r.getStatus().getHealth().equals(HealthType.Failed) && !r.getStatus().getHealth().equals(HealthType.Completed)) {
                r.getStatus().setHealth(ht);
              }

              if (run.has(IlluminaTransformer.JSON_STATUS)) {
                r.getStatus().setXml(run.getString(IlluminaTransformer.JSON_STATUS));
              } else {
                log.debug("No new status XML information coming through from notification system...");
              }
            } else {
              if (run.has(IlluminaTransformer.JSON_STATUS)) {
                is.setXml(run.getString(IlluminaTransformer.JSON_STATUS));
              }

              is.setHealth(ht);
              r.setStatus(is);
            }

            log.debug(runName + " New status: " + r.getStatus().getHealth().toString() + " -> " + ht.toString());

            if (run.has(IlluminaTransformer.JSON_NUM_CYCLES)) {
              r.setCycles(Integer.parseInt(run.getString(IlluminaTransformer.JSON_NUM_CYCLES)));
            }

            if (r.getSequencerReference() == null) {
              SequencerReference sr = null;
              if (run.has(IlluminaTransformer.JSON_SEQUENCER_NAME)) {
                sr = requestManager.getSequencerReferenceByName(run.getString(IlluminaTransformer.JSON_SEQUENCER_NAME));
                r.getStatus().setInstrumentName(run.getString(IlluminaTransformer.JSON_SEQUENCER_NAME));
                r.setSequencerReference(sr);
              }
              if (r.getSequencerReference() == null) {
                sr = requestManager.getSequencerReferenceByName(m.group(1));
                r.setSequencerReference(sr);
              }
              if (r.getSequencerReference() == null) {
                sr = requestManager.getSequencerReferenceByName(r.getStatus().getInstrumentName());
                r.setSequencerReference(sr);
              }
            }

            if (run.has(IlluminaTransformer.JSON_START_DATE)) {
              try {
                if (!"null".equals(run.getString(IlluminaTransformer.JSON_START_DATE)) && !isStringEmptyOrNull(run.getString(IlluminaTransformer.JSON_START_DATE))) {
                  log.debug("Updating start date:" + run.getString(IlluminaTransformer.JSON_START_DATE));
                  r.getStatus().setStartDate(illuminaRunFolderDateFormat.parse(run.getString(IlluminaTransformer.JSON_START_DATE)));
                }
              } catch (ParseException e) {
                log.error(runName, e);
              }
            }

            if (run.has(IlluminaTransformer.JSON_COMPLETE_DATE)) {
              if (!"null".equals(run.getString(IlluminaTransformer.JSON_COMPLETE_DATE)) && !isStringEmptyOrNull(run.getString(IlluminaTransformer.JSON_COMPLETE_DATE))) {
                log.debug("Updating completion date:" + run.getString(IlluminaTransformer.JSON_COMPLETE_DATE));
                try {
                  r.getStatus().setCompletionDate(logDateFormat.parse(run.getString(IlluminaTransformer.JSON_COMPLETE_DATE)));
                } catch (ParseException e) {
                  log.error(runName, e);
                  try {
                    r.getStatus().setCompletionDate(anotherLogDateFormat.parse(run.getString(IlluminaTransformer.JSON_COMPLETE_DATE)));
                  } catch (ParseException e1) {
                    log.error(runName, e1);
                  }
                }
              } else {
                if (!r.getStatus().getHealth().equals(HealthType.Completed) && !r.getStatus().getHealth().equals(HealthType.Failed)
                    && !r.getStatus().getHealth().equals(HealthType.Stopped)) {
                  r.getStatus().setCompletionDate(null);
                }
              }
            }

            // update path if changed
            if (run.has(IlluminaTransformer.JSON_FULL_PATH) && !isStringEmptyOrNull(run.getString(IlluminaTransformer.JSON_FULL_PATH)) && !isStringEmptyOrNull(r.getFilePath())) {
              if (!run.getString(IlluminaTransformer.JSON_FULL_PATH).equals(r.getFilePath())) {
                log.debug("Updating run file path:" + r.getFilePath() + " -> " + run.getString(IlluminaTransformer.JSON_FULL_PATH));
                r.setFilePath(run.getString(IlluminaTransformer.JSON_FULL_PATH));
              }
            }
          }

          if (r.getSequencerReference() != null) {
            processRunParams(run, r);
            Collection<SequencerPartitionContainer<SequencerPoolPartition>> fs = r.getSequencerPartitionContainers();
            if (fs.isEmpty()) {
              if (run.has("containerId") && !isStringEmptyOrNull(run.getString("containerId"))) {
                Collection<SequencerPartitionContainer<SequencerPoolPartition>> pfs = requestManager
                    .listSequencerPartitionContainersByBarcode(run.getString("containerId"));
                if (!pfs.isEmpty()) {
                  if (pfs.size() == 1) {
                    SequencerPartitionContainer<SequencerPoolPartition> lf = new ArrayList<SequencerPartitionContainer<SequencerPoolPartition>>(
                        pfs).get(0);
                    if (lf.getSecurityProfile() != null && r.getSecurityProfile() == null) {
                      r.setSecurityProfile(lf.getSecurityProfile());
                    }
                    if (lf.getPlatform() == null && r.getSequencerReference().getPlatform() != null) {
                      lf.setPlatform(r.getSequencerReference().getPlatform());
                    }

                    if (run.has(IlluminaTransformer.JSON_LANE_COUNT) && run.getInt(IlluminaTransformer.JSON_LANE_COUNT) != lf.getPartitions().size()) {
                      log.warn(r.getAlias()
                          + ":: Previously saved flowcell lane count does not match notification-supplied value from RunInfo.xml. Setting new partitionLimit");
                      lf.setPartitionLimit(run.getInt(IlluminaTransformer.JSON_LANE_COUNT));
                    }

                    r.addSequencerPartitionContainer(lf);
                  } else {
                    // more than one flowcell hit to this barcode
                    log.warn(r.getAlias()
                        + ":: More than one partition container has this barcode. Cannot automatically link to a pre-existing barcode.");
                  }
                } else {
                  SequencerPartitionContainer<SequencerPoolPartition> f = new SequencerPartitionContainerImpl();
                  f.setSecurityProfile(r.getSecurityProfile());
                  if (f.getPlatform() == null && r.getSequencerReference().getPlatform() != null) {
                    f.setPlatform(r.getSequencerReference().getPlatform());
                  }

                  if (run.has(IlluminaTransformer.JSON_LANE_COUNT)) {
                    f.setPartitionLimit(run.getInt(IlluminaTransformer.JSON_LANE_COUNT));
                  } else {
                    if (r.getSequencerReference().getPlatform().getInstrumentModel().contains("MiSeq")) {
                      f.setPartitionLimit(1);
                    }
                  }

                  f.initEmptyPartitions();
                  f.setIdentificationBarcode(run.getString("containerId"));
                  r.addSequencerPartitionContainer(f);
                }
              }
            } else {
              SequencerPartitionContainer<SequencerPoolPartition> f = fs.iterator().next();
              f.setSecurityProfile(r.getSecurityProfile());
              if (f.getPlatform() == null && r.getSequencerReference().getPlatform() != null) {
                f.setPlatform(r.getSequencerReference().getPlatform());
              }

              if (f.getPartitions().isEmpty()) {
                if (run.has(IlluminaTransformer.JSON_LANE_COUNT)) {
                  f.setPartitionLimit(run.getInt(IlluminaTransformer.JSON_LANE_COUNT));
                } else {
                  String instrumentModel = r.getSequencerReference().getPlatform().getInstrumentModel();
                  if (instrumentModel.contains("MiSeq") || instrumentModel.contains("NextSeq")) {
                    f.setPartitionLimit(1);
                  }
                }
                f.initEmptyPartitions();
              } else {
                if (r.getSequencerReference().getPlatform().getInstrumentModel().contains("MiSeq")) {
                  if (f.getPartitions().size() != 1) {
                    log.warn(f.getName() + ":: WARNING - number of partitions found (" + f.getPartitions().size()
                        + ") doesn't match usual number of MiSeq/NextSeq partitions (1)");
                  }
                } else if (r.getSequencerReference().getPlatform().getInstrumentModel().contains("2500")) {
                  if (f.getPartitions().size() != 2 && f.getPartitions().size() != 8) {
                    log.warn(f.getName() + ":: WARNING - number of partitions found (" + f.getPartitions().size()
                        + ") doesn't match usual number of HiSeq 2500 partitions (2/8)");
                  }
                } else {
                  if (f.getPartitions().size() != 8) {
                    log.warn(f.getName() + ":: WARNING - number of partitions found (" + f.getPartitions().size()
                        + ") doesn't match usual number of GA/HiSeq partitions (8)");
                    log.warn("Attempting fix...");
                    Map<Integer, Partition> parts = new HashMap<Integer, Partition>();
                    Partition notNullPart = f.getPartitions().get(0);
                    long notNullPartID = notNullPart.getId();
                    int notNullPartNum = notNullPart.getPartitionNumber();

                    for (int i = 1; i < 9; i++) {
                      parts.put(i, null);
                    }

                    for (Partition p : f.getPartitions()) {
                      parts.put(p.getPartitionNumber(), p);
                    }

                    for (Integer num : parts.keySet()) {
                      if (parts.get(num) == null) {
                        long newId = (notNullPartID - notNullPartNum) + num;
                        log.info("Inserting partition at " + num + " with ID " + newId);
                        SequencerPoolPartition p = new PartitionImpl();
                        p.setSequencerPartitionContainer(f);
                        p.setId(newId);
                        p.setPartitionNumber(num);
                        p.setSecurityProfile(f.getSecurityProfile());
                        ((SequencerPartitionContainerImpl) f).addPartition(p);
                      }
                    }

                    log.info(f.getName() + ":: partitions now (" + f.getPartitions().size() + ")");
                  }
                }
              }

              if (isStringEmptyOrNull(f.getIdentificationBarcode())) {
                if (run.has(IlluminaTransformer.JSON_CONTAINER_ID) && !isStringEmptyOrNull(run.getString(IlluminaTransformer.JSON_CONTAINER_ID))) {
                  f.setIdentificationBarcode(run.getString(IlluminaTransformer.JSON_CONTAINER_ID));
                }
              }
            }

            updatedRuns.put(r.getAlias(), r);
            runsToSave.add(r);
          }
        } else {
          log.warn("\\_ Run not saved. Saving status: " + is.getRunName());
          requestManager.saveStatus(is);
        }
      } catch (IOException ioe) {
        log.error("Couldn't process run", ioe);
      }
    }
    try {
      if (runsToSave.size() > 0) {
        int[] saved = requestManager.saveRuns(runsToSave);
        log.info("Batch saved " + saved.length + " / " + runs.size() + " runs");
      }
    } catch (IOException e) {
      log.error("Couldn't save run batch", e);
    }

    return updatedRuns;
  }
  
  public void processRunParams(JSONObject run, Run r) {
    if (run.has(IlluminaTransformer.JSON_RUN_PARAMS) && r.getSequencingParametersId() == null) {
      Document document;
      try {
          document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
              .parse(new InputSource(new StringReader(run.getString(IlluminaTransformer.JSON_RUN_PARAMS))));

        for (SequencingParameters parameters : getParameterSet()) {
          log.debug("Checking run " + run.getString(IlluminaTransformer.JSON_RUN_NAME) + " against parameters " + parameters.getName());
       
          if (parameters.getPlatformId() == r.getSequencerReference().getPlatform().getId() && parameters.matches(document)) {
            log.debug("Matched run " + run.getString(IlluminaTransformer.JSON_RUN_NAME) + " to parameters " + parameters.getName());
            r.setSequencingParametersId(parameters.getId());
            break;
          }
        }
      } catch (SAXException | ParserConfigurationException | XPathExpressionException | IOException e) {
        log.error("Error parsing runparams", e);
      }
    } else {
      log.debug("No run parameters: " + run.getString(IlluminaTransformer.JSON_RUN_NAME));
    }
  }

  public SequencingParametersCollection getParameterSet() {
    return parameterSet;
  }

  public void setParameterSet(SequencingParametersCollection parameterSet) {
    this.parameterSet = parameterSet;
  }
}
