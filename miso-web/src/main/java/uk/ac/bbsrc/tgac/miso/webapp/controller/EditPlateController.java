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

package uk.ac.bbsrc.tgac.miso.webapp.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.ModelAndView;

import uk.ac.bbsrc.tgac.miso.core.data.AbstractPlate;
import uk.ac.bbsrc.tgac.miso.core.data.ChangeLog;
import uk.ac.bbsrc.tgac.miso.core.data.Plate;
import uk.ac.bbsrc.tgac.miso.core.data.Plateable;
import uk.ac.bbsrc.tgac.miso.core.factory.DataObjectFactory;
import uk.ac.bbsrc.tgac.miso.core.manager.RequestManager;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;

import com.eaglegenomics.simlims.core.User;
import com.eaglegenomics.simlims.core.manager.SecurityManager;

@Controller
@RequestMapping("/plate")
@SessionAttributes("plate")
public class EditPlateController {
  protected static final Logger log = LoggerFactory.getLogger(EditPlateController.class);

  @Autowired
  private SecurityManager securityManager;

  @Autowired
  private RequestManager requestManager;

  @Autowired
  private DataObjectFactory dataObjectFactory;

  public void setDataObjectFactory(DataObjectFactory dataObjectFactory) {
    this.dataObjectFactory = dataObjectFactory;
  }

  public void setRequestManager(RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  @ModelAttribute("materialTypes")
  public Collection<String> populateMaterialTypes() throws IOException {
    return requestManager.listAllStudyTypes();
  }

  @ModelAttribute("maxLengths")
  public Map<String, Integer> maxLengths() throws IOException {
    return requestManager.getPlateColumnSizes();
  }

  @Value("${miso.autoGenerateIdentificationBarcodes}")
  private Boolean autoGenerateIdBarcodes;

  @ModelAttribute("autoGenerateIdBarcodes")
  public Boolean autoGenerateIdentificationBarcodes() {
    return autoGenerateIdBarcodes;
  }

  @RequestMapping(value = "/new", method = RequestMethod.GET)
  public ModelAndView newPlate(ModelMap model) throws IOException {
    return setupForm(AbstractPlate.UNSAVED_ID, model);
  }

  @RequestMapping(value = "/rest/{plateId}", method = RequestMethod.GET)
  public @ResponseBody Plate<? extends List<? extends Plateable>, ? extends Plateable> jsonRest(@PathVariable Long plateId)
      throws IOException {
    return requestManager.getPlateById(plateId);
  }

  @RequestMapping(value = "/rest/changes", method = RequestMethod.GET)
  public @ResponseBody Collection<ChangeLog> jsonRestChanges() throws IOException {
    return requestManager.listAllChanges("Plate");
  }

  @RequestMapping(value = "/{plateId}", method = RequestMethod.GET)
  public ModelAndView setupForm(@PathVariable Long plateId, ModelMap model) throws IOException {
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      Plate<? extends List<? extends Plateable>, ? extends Plateable> plate = null;
      if (plateId == AbstractPlate.UNSAVED_ID) {
        plate = dataObjectFactory.getPlateOfSize(96, user);
        model.put("title", "New Plate");
      } else {
        plate = requestManager.getPlateById(plateId);
        model.put("title", "Plate " + plateId);
      }

      if (plate != null) {
        if (!plate.userCanRead(user)) {
          throw new SecurityException("Permission denied.");
        }
        model.put("formObj", plate);
        model.put("plate", plate);
      } else {
        throw new SecurityException("No such Plate");
      }
      return new ModelAndView("/pages/editPlate.jsp", model);
    } catch (IOException ex) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to show Plate", ex);
      }
      throw ex;
    }
  }

  @RequestMapping(value = "/import", method = RequestMethod.GET)
  public ModelAndView importPlate(ModelMap model) throws IOException {
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      Plate<? extends List<? extends Plateable>, ? extends Plateable> plate = dataObjectFactory.getPlateOfSize(96, user);
      model.put("title", "Import Plate");
      model.put("formObj", plate);
      model.put("plate", plate);
      return new ModelAndView("/pages/importPlate.jsp", model);
    } catch (IOException ex) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to show Plate", ex);
      }
      throw ex;
    }
  }

  @RequestMapping(value = "/export", method = RequestMethod.GET)
  public ModelAndView exportPlate(ModelMap model) throws IOException {
    return new ModelAndView("/pages/exportPlate.jsp", model);
  }

  @RequestMapping(method = RequestMethod.POST)
  public String processSubmit(@ModelAttribute("plate") Plate<LinkedList<Plateable>, Plateable> plate, ModelMap model, SessionStatus session)
      throws IOException {
    try {
      User user = securityManager.getUserByLoginName(SecurityContextHolder.getContext().getAuthentication().getName());
      if (!plate.userCanWrite(user)) {
        throw new SecurityException("Permission denied.");
      }
      plate.setLastModifier(user);
      requestManager.savePlate(plate);
      session.setComplete();
      model.clear();
      return "redirect:/miso/plate/" + plate.getId();
    } catch (IOException ex) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to save Plate", ex);
      }
      throw ex;
    }
  }
}
