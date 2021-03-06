package uk.ac.bbsrc.tgac.miso.dto;

import org.codehaus.jackson.map.annotate.JsonSerialize;

public class DilutionDto {

  private Long id;
  private String name;
  private String identificationBarcode;
  private String locationLabel;
  private LibraryDto library;
  private String libraryUrl;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
  public String getIdentificationBarcode() {
    return identificationBarcode;
  }

  public void setIdentificationBarcode(String identificationBarcode) {
    this.identificationBarcode = identificationBarcode;
  }

  public String getLocationLabel() {
    return locationLabel;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)
  public void setLocationLabel(String locationLabel) {
    this.locationLabel = locationLabel;
  }

  public LibraryDto getLibrary() {
    return library;
  }

  public void setLibrary(LibraryDto library) {
    this.library = library;
  }

  public String getLibraryUrl() {
    return libraryUrl;
  }

  public void setLibraryUrl(String libraryUrl) {
    this.libraryUrl = libraryUrl;
  }

}
