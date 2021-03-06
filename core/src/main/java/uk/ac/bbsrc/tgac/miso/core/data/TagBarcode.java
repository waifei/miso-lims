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

package uk.ac.bbsrc.tgac.miso.core.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * TagBarcodes represent adapter sequences that can be prepended to sequencable material in order to facilitate multiplexing.
 * 
 * @author Rob Davey
 * @date 10-May-2011
 * @since 0.0.3
 */
@JsonSerialize(typing = JsonSerialize.Typing.STATIC, include = JsonSerialize.Inclusion.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@Entity
@Table(name = "TagBarcodes")
public class TagBarcode implements Nameable {

  public static final Long UNSAVED_ID = 0L;

  public static void sort(final List<TagBarcode> barcodes) {
    Collections.sort(barcodes, new Comparator<TagBarcode>() {
      @Override
      public int compare(TagBarcode o1, TagBarcode o2) {
        return o1.getPosition() - o2.getPosition();
      }
    });
  }

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "tagFamilyId", nullable = false)
  @JsonBackReference
  private TagBarcodeFamily family;
  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private int position;
  @Column(nullable = false)
  private String sequence;

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private long tagId = UNSAVED_ID;

  public TagBarcodeFamily getFamily() {
    return family;
  }

  @Override
  public long getId() {
    return tagId;
  }

  @Override
  public String getName() {
    return name;
  }

  public int getPosition() {
    return position;
  }

  public String getSequence() {
    return sequence;
  }

  public void setFamily(TagBarcodeFamily family) {
    this.family = family;
  }

  public void setId(long id) {
    this.tagId = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public void setSequence(String sequence) {
    this.sequence = sequence;
  }

  public String getLabel() {
    if (getId() != UNSAVED_ID) {
      return getName() + " (" + getSequence() + ")";
    } else {
      return null;
    }
  }

}
