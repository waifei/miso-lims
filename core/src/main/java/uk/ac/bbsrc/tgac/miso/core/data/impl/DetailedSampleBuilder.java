package uk.ac.bbsrc.tgac.miso.core.data.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.w3c.dom.Document;

import com.eaglegenomics.simlims.core.Note;
import com.eaglegenomics.simlims.core.SecurityProfile;
import com.eaglegenomics.simlims.core.User;
import com.google.common.collect.Lists;

import uk.ac.bbsrc.tgac.miso.core.data.AbstractSample;
import uk.ac.bbsrc.tgac.miso.core.data.ChangeLog;
import uk.ac.bbsrc.tgac.miso.core.data.Identity;
import uk.ac.bbsrc.tgac.miso.core.data.Lab;
import uk.ac.bbsrc.tgac.miso.core.data.Library;
import uk.ac.bbsrc.tgac.miso.core.data.Project;
import uk.ac.bbsrc.tgac.miso.core.data.QcPassedDetail;
import uk.ac.bbsrc.tgac.miso.core.data.SampleAdditionalInfo;
import uk.ac.bbsrc.tgac.miso.core.data.SampleAliquot;
import uk.ac.bbsrc.tgac.miso.core.data.SampleCVSlide;
import uk.ac.bbsrc.tgac.miso.core.data.SampleClass;
import uk.ac.bbsrc.tgac.miso.core.data.SampleLCMTube;
import uk.ac.bbsrc.tgac.miso.core.data.SamplePurpose;
import uk.ac.bbsrc.tgac.miso.core.data.SampleQC;
import uk.ac.bbsrc.tgac.miso.core.data.SampleStock;
import uk.ac.bbsrc.tgac.miso.core.data.SampleTissue;
import uk.ac.bbsrc.tgac.miso.core.data.SampleTissueProcessing;
import uk.ac.bbsrc.tgac.miso.core.data.Subproject;
import uk.ac.bbsrc.tgac.miso.core.data.TissueMaterial;
import uk.ac.bbsrc.tgac.miso.core.data.TissueOrigin;
import uk.ac.bbsrc.tgac.miso.core.data.TissueType;
import uk.ac.bbsrc.tgac.miso.core.data.impl.kit.KitDescriptor;
import uk.ac.bbsrc.tgac.miso.core.data.type.StrStatus;
import uk.ac.bbsrc.tgac.miso.core.exception.MalformedLibraryException;
import uk.ac.bbsrc.tgac.miso.core.exception.MalformedSampleQcException;
import uk.ac.bbsrc.tgac.miso.core.exception.ReportingException;
import uk.ac.bbsrc.tgac.miso.core.security.SecurableByProfile;

public class DetailedSampleBuilder implements SampleAdditionalInfo, SampleAliquot, SampleStock, SampleTissue, SampleTissueProcessing,
    SampleCVSlide, SampleLCMTube, Identity {

  @SuppressWarnings("unused")
  private static final long serialVersionUID = 1L;

  // Sample attributes
  private long sampleId = AbstractSample.UNSAVED_ID;
  private Project project;
  private SecurityProfile securityProfile = null;
  private String accession;
  private String name;
  private String description;
  private String scientificName;
  private String taxonIdentifier;
  private String sampleType;
  private Date receivedDate;
  private Boolean qcPassed;
  private String identificationBarcode;
  private String locationBarcode;
  private String alias;
  private Long securityProfile_profileId;
  private User lastModifier;
  private Double volume;
  private boolean emptied = false;
  private boolean isSynthetic = false;
  private final Collection<ChangeLog> changeLog = new ArrayList<>();

  // DetailedSample attributes
  private SampleAdditionalInfo parent;
  private SampleClass sampleClass;
  private QcPassedDetail qcPassedDetail;
  private Subproject subproject;
  private Long kitDescriptorId;
  private KitDescriptor prepKit;
  private Boolean archived = Boolean.FALSE;
  private Long groupId;
  private String groupDescription;
  private Integer siblingNumber;

  // Identity attributes
  private String internalName;
  private String externalName;
  private DonorSex donorSex = DonorSex.UNKNOWN;

  // TissueSample attributes
  private SampleClass tissueClass; // identifies a parent tissue class if this sample itself is not a tissue
  private TissueOrigin tissueOrigin;
  private TissueType tissueType;
  private String externalInstituteIdentifier;
  private Lab lab;
  private Integer passageNumber;
  private Integer timesReceived;
  private Integer tubeNumber;
  private TissueMaterial tissueMaterial;
  private String region;

  // AnalyteAliquot attributes
  private SamplePurpose samplePurpose;

  // SampleStock attributes
  private StrStatus strStatus = StrStatus.NOT_SUBMITTED;
  private Double concentration;

  // TissueProcessingSample attributes
  // CV Slide
  private Integer cuts;
  private Integer discards;
  private Integer thickness;
  // LCM Tube
  private Integer cutsConsumed;

  public DetailedSampleBuilder() {
    this(null);
  }

  public DetailedSampleBuilder(User user) {
    if (user != null) {
      securityProfile = new SecurityProfile(user);
      securityProfile_profileId = getSecurityProfile().getProfileId();
    }
  }

  @Override
  public long getId() {
    return sampleId;
  }

  @Override
  public void setId(long id) {
    this.sampleId = id;
  }

  @Override
  public Project getProject() {
    return project;
  }

  @Override
  public void setProject(Project project) {
    this.project = project;
  }

  @Override
  public Collection<SampleQC> getSampleQCs() {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public void setQCs(Collection<SampleQC> sampleQCs) {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public Collection<Note> getNotes() {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public void setNotes(Collection<Note> notes) {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  public Document getSubmissionDocument() {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  public void setSubmissionDocument(Document submissionDocument) {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public SecurityProfile getSecurityProfile() {
    return securityProfile;
  }

  @Override
  public void setSecurityProfile(SecurityProfile securityProfile) {
    this.securityProfile = securityProfile;
  }

  @Override
  public String getAccession() {
    return accession;
  }

  @Override
  public void setAccession(String accession) {
    this.accession = accession;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public String getScientificName() {
    return scientificName;
  }

  @Override
  public void setScientificName(String scientificName) {
    this.scientificName = scientificName;
  }

  @Override
  public String getTaxonIdentifier() {
    return taxonIdentifier;
  }

  @Override
  public void setTaxonIdentifier(String taxonIdentifier) {
    this.taxonIdentifier = taxonIdentifier;
  }

  @Override
  public String getSampleType() {
    return sampleType;
  }

  @Override
  public void setSampleType(String sampleType) {
    this.sampleType = sampleType;
  }

  @Override
  public Date getReceivedDate() {
    return receivedDate;
  }

  @Override
  public void setReceivedDate(Date receivedDate) {
    this.receivedDate = receivedDate;
  }

  @Override
  public Boolean getQcPassed() {
    return qcPassed;
  }

  @Override
  public void setQcPassed(Boolean qcPassed) {
    this.qcPassed = qcPassed;
  }

  @Override
  public String getIdentificationBarcode() {
    return identificationBarcode;
  }

  @Override
  public void setIdentificationBarcode(String identificationBarcode) {
    this.identificationBarcode = identificationBarcode;
  }

  @Override
  public String getLocationBarcode() {
    return locationBarcode;
  }

  @Override
  public void setLocationBarcode(String locationBarcode) {
    this.locationBarcode = locationBarcode;
  }

  @Override
  public String getAlias() {
    return alias;
  }

  @Override
  public void setAlias(String alias) {
    this.alias = alias;
  }

  @Override
  public Long getSecurityProfileId() {
    return securityProfile_profileId;
  }

  @Override
  public void setSecurityProfileId(Long securityProfileId) {
    this.securityProfile_profileId = securityProfileId;
  }

  @Override
  public User getLastModifier() {
    return lastModifier;
  }

  @Override
  public void setLastModifier(User lastModifier) {
    this.lastModifier = lastModifier;
  }

  @Override
  public SampleAdditionalInfo getParent() {
    return parent;
  }

  @Override
  public void setParent(SampleAdditionalInfo parent) {
    this.parent = parent;
  }

  @Override
  public Set<SampleAdditionalInfo> getChildren() {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public void setChildren(Set<SampleAdditionalInfo> children) {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public SampleClass getSampleClass() {
    return sampleClass;
  }

  @Override
  public void setSampleClass(SampleClass sampleClass) {
    this.sampleClass = sampleClass;
  }

  @Override
  public TissueOrigin getTissueOrigin() {
    return tissueOrigin;
  }

  @Override
  public void setTissueOrigin(TissueOrigin tissueOrigin) {
    this.tissueOrigin = tissueOrigin;
  }

  @Override
  public TissueType getTissueType() {
    return tissueType;
  }

  @Override
  public void setTissueType(TissueType tissueType) {
    this.tissueType = tissueType;
  }

  @Override
  public QcPassedDetail getQcPassedDetail() {
    return qcPassedDetail;
  }

  @Override
  public void setQcPassedDetail(QcPassedDetail qcPassedDetail) {
    this.qcPassedDetail = qcPassedDetail;
  }

  @Override
  public Subproject getSubproject() {
    return subproject;
  }

  @Override
  public void setSubproject(Subproject subproject) {
    this.subproject = subproject;
  }

  @Override
  public String getExternalInstituteIdentifier() {
    return externalInstituteIdentifier;
  }

  @Override
  public void setExternalInstituteIdentifier(String externalInstituteIdentifier) {
    this.externalInstituteIdentifier = externalInstituteIdentifier;
  }

  @Override
  public Lab getLab() {
    return lab;
  }

  @Override
  public void setLab(Lab lab) {
    this.lab = lab;
  }

  public Long getKitDescriptorId() {
    return kitDescriptorId;
  }

  public void setKitDescriptorId(Long kitDescriptorId) {
    this.kitDescriptorId = kitDescriptorId;
  }

  @Override
  public KitDescriptor getPrepKit() {
    return prepKit;
  }

  @Override
  public void setPrepKit(KitDescriptor prepKit) {
    this.prepKit = prepKit;
  }

  @Override
  public Integer getPassageNumber() {
    return passageNumber;
  }

  @Override
  public void setPassageNumber(Integer passageNumber) {
    this.passageNumber = passageNumber;
  }

  @Override
  public Integer getTimesReceived() {
    return timesReceived;
  }

  @Override
  public void setTimesReceived(Integer timesReceived) {
    this.timesReceived = timesReceived;
  }

  @Override
  public Integer getTubeNumber() {
    return tubeNumber;
  }

  @Override
  public void setTubeNumber(Integer tubeNumber) {
    this.tubeNumber = tubeNumber;
  }

  @Override
  public Double getConcentration() {
    return concentration;
  }

  @Override
  public void setConcentration(Double concentration) {
    this.concentration = concentration;
  }

  @Override
  public Boolean getArchived() {
    return archived;
  }

  @Override
  public void setArchived(Boolean archived) {
    this.archived = archived;
  }

  @Override
  public Integer getSiblingNumber() {
    return siblingNumber;
  }

  @Override
  public void setSiblingNumber(Integer siblingNumber) {
    this.siblingNumber = siblingNumber;
  }

  @Override
  public Long getGroupId() {
    return groupId;
  }

  @Override
  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }

  @Override
  public String getGroupDescription() {
    return groupDescription;
  }

  @Override
  public void setGroupDescription(String groupDescription) {
    this.groupDescription = groupDescription;
  }

  @Override
  public String getInternalName() {
    return internalName;
  }

  @Override
  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  @Override
  public String getExternalName() {
    return externalName;
  }

  @Override
  public void setExternalName(String externalName) {
    this.externalName = externalName;
  }

  @Override
  public DonorSex getDonorSex() {
    return donorSex;
  }

  @Override
  public void setDonorSex(DonorSex donorSex) {
    this.donorSex = donorSex;
  }

  @Override
  public Integer getCuts() {
    return cuts;
  }

  @Override
  public void setCuts(Integer cuts) {
    this.cuts = cuts;
  }

  @Override
  public Integer getCutsRemaining() {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public Integer getDiscards() {
    return discards;
  }

  @Override
  public void setDiscards(Integer discards) {
    this.discards = discards;
  }

  @Override
  public Integer getThickness() {
    return thickness;
  }

  @Override
  public void setThickness(Integer thickness) {
    this.thickness = thickness;
  }

  @Override
  public Integer getCutsConsumed() {
    return cutsConsumed;
  }

  @Override
  public void setCutsConsumed(Integer cutsConsumed) {
    this.cutsConsumed = cutsConsumed;
  }

  @Override
  public SamplePurpose getSamplePurpose() {
    return samplePurpose;
  }

  @Override
  public void setSamplePurpose(SamplePurpose samplePurpose) {
    this.samplePurpose = samplePurpose;
  }

  @Override
  public TissueMaterial getTissueMaterial() {
    return tissueMaterial;
  }

  @Override
  public void setTissueMaterial(TissueMaterial tissueMaterial) {
    this.tissueMaterial = tissueMaterial;
  }

  @Override
  public StrStatus getStrStatus() {
    return strStatus;
  }

  @Override
  public void setStrStatus(StrStatus strStatus) {
    this.strStatus = strStatus;
  }

  @Override
  public String getRegion() {
    return region;
  }

  @Override
  public void setRegion(String region) {
    this.region = region;
  }

  @Override
  public Collection<ChangeLog> getChangeLog() {
    return changeLog;
  }

  @Override
  public void addNote(Note note) {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public void addQc(SampleQC sampleQc) throws MalformedSampleQcException {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public void inheritPermissions(SecurableByProfile parent) throws SecurityException {
    if (parent.getSecurityProfile().getOwner() != null) {
      setSecurityProfile(parent.getSecurityProfile());
    } else {
      throw new SecurityException("Cannot inherit permissions when parent object owner is not set!");
    }
  }

  @Override
  public boolean userCanRead(User arg0) {
    return true;
  }

  @Override
  public boolean userCanWrite(User arg0) {
    return true;
  }

  @Override
  public void buildSubmission() {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public void buildReport() throws ReportingException {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public int compareTo(Object o) {
    return 0;
  }

  @Override
  public boolean isDeletable() {
    return false;
  }

  @Override
  public void setBoxId(Long boxId) {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public Long getBoxId() {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public void setBoxAlias(String alias) {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public String getBoxAlias() {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public void setEmpty(boolean emptied) {
    this.emptied = emptied;
  }

  @Override
  public boolean isEmpty() {
    return emptied;
  }

  @Override
  public Double getVolume() {
    return volume;
  }

  @Override
  public void setVolume(Double volume) {
    this.volume = volume;
  }

  @Override
  public Long getBoxPositionId() {
    return null;
  }

  @Override
  public void setBoxPositionId(Long id) {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public String getBoxPosition() {
    return null;
  }

  @Override
  public void setBoxPosition(String id) {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public String getBoxLocation() {
    return null;
  }

  @Override
  public void setBoxLocation(String boxLocation) {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public String getLabelText() {
    return null;
  }

  @Override
  public void setDonorSex(String donorSex) {
    this.donorSex = DonorSex.valueOf(donorSex);
  }

  @Override
  public void setStrStatus(String strStatus) {
    this.strStatus = StrStatus.get(strStatus);
  }

  @Override
  public Long getHibernateKitDescriptorId() {
    return this.kitDescriptorId;
  }

  @Override
  public void addLibrary(Library library) throws MalformedLibraryException {
    throw new UnsupportedOperationException("Method not implemented on builder");
  }

  @Override
  public Collection<Library> getLibraries() {
    return Lists.newArrayList();
  }

  public SampleClass getTissueClass() {
    return tissueClass;
  }

  public void setTissueClass(SampleClass tissueClass) {
    this.tissueClass = tissueClass;
  }

  @Override
  public Date getLastModified() {
    return new Date();
  }

  @Override
  public Boolean isSynthetic() {
    return isSynthetic;
  }

  @Override
  public void setSynthetic(Boolean isSynthetic) {
    this.isSynthetic = isSynthetic;
  }

  public SampleAdditionalInfo build() {
    if (sampleClass == null || sampleClass.getSampleCategory() == null) {
      throw new NullPointerException("Missing sample class or category");
    }
    SampleAdditionalInfo sample = null;
    switch (sampleClass.getSampleCategory()) {
    case Identity.CATEGORY_NAME:
      Identity identity = new IdentityImpl();
      identity.setInternalName(internalName);
      identity.setExternalName(externalName);
      identity.setDonorSex(donorSex);
      sample = identity;
      break;
    case SampleTissue.CATEGORY_NAME:
      sample = buildTissue();
      break;
    case SampleTissueProcessing.CATEGORY_NAME:
      if (sampleClass.getAlias() == SampleCVSlide.SAMPLE_CLASS_NAME) {
        SampleCVSlide cvSlide = new SampleCVSlideImpl();
        cvSlide.setCuts(cuts);
        cvSlide.setDiscards(discards);
        cvSlide.setThickness(thickness);
        sample = cvSlide;
      } else if (sampleClass.getAlias() == SampleLCMTube.SAMPLE_CLASS_NAME) {
        SampleLCMTube lcmTube = new SampleLCMTubeImpl();
        lcmTube.setCutsConsumed(cutsConsumed);
        sample = lcmTube;
      } else {
        SampleTissueProcessing processing = new SampleTissueProcessingImpl();
        sample = processing;
      }
      break;
    case SampleStock.CATEGORY_NAME:
      SampleStock stock = new SampleStockImpl();
      stock.setStrStatus(strStatus);
      stock.setConcentration(concentration);
      sample = stock;
      break;
    case SampleAliquot.CATEGORY_NAME:
      SampleAliquot aliquot = new SampleAliquotImpl();
      aliquot.setSamplePurpose(samplePurpose);
      sample = aliquot;
      break;
    default:
      throw new IllegalArgumentException("Unknown sample category: " + sampleClass.getSampleCategory());
    }

    if (parent != null) {
      sample.setParent(parent);
    } else if (!Identity.CATEGORY_NAME.equals(sampleClass.getSampleCategory())) {
      Identity identity = new IdentityImpl();
      identity.setExternalName(externalName);
      identity.setInternalName(internalName);
      identity.setDonorSex(donorSex);
      if (SampleTissue.CATEGORY_NAME.equals(sampleClass.getSampleCategory())) {
        sample.setParent(identity);
      } else {
        if (tissueClass == null) throw new NullPointerException("Missing tissue class");
        SampleTissue tissue = buildTissue();
        tissue.setSampleClass(tissueClass);
        tissue.setParent(identity);
        sample.setParent(tissue);
      }
    }

    sample.setId(sampleId);
    sample.setProject(project);
    sample.setSecurityProfileId(securityProfile_profileId);
    sample.setSecurityProfile(securityProfile);
    sample.setAccession(accession);
    sample.setName(name);
    sample.setDescription(description);
    sample.setScientificName(scientificName);
    sample.setTaxonIdentifier(taxonIdentifier);
    sample.setSampleType(sampleType);
    sample.setReceivedDate(receivedDate);
    sample.setQcPassed(qcPassed);
    sample.setIdentificationBarcode(identificationBarcode);
    sample.setLocationBarcode(locationBarcode);
    sample.setAlias(alias);
    sample.setLastModifier(lastModifier);
    sample.setVolume(volume);
    sample.setEmpty(emptied);
    sample.getChangeLog().addAll(changeLog);

    sample.setSampleClass(sampleClass);
    sample.setQcPassedDetail(qcPassedDetail);
    sample.setSubproject(subproject);
    sample.setPrepKit(prepKit);
    sample.setArchived(archived);
    sample.setGroupId(groupId);
    sample.setGroupDescription(groupDescription);
    sample.setSynthetic(isSynthetic);
    sample.setSiblingNumber(siblingNumber);

    return sample;
  }

  private SampleTissue buildTissue() {
    SampleTissue tissue = new SampleTissueImpl();
    tissue.setTimesReceived(timesReceived);
    tissue.setTubeNumber(tubeNumber);
    tissue.setPassageNumber(passageNumber);
    tissue.setTissueType(tissueType);
    tissue.setTissueOrigin(tissueOrigin);
    tissue.setExternalInstituteIdentifier(externalInstituteIdentifier);
    tissue.setLab(lab);
    tissue.setTissueMaterial(tissueMaterial);
    tissue.setRegion(region);
    return tissue;
  }

}
