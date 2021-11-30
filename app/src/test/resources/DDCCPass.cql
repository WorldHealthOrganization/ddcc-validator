library TestArtifact version '1'

using FHIR version '4.0.0'

include "FHIRHelpers" version '4.0.0' called FHIRHelpers

context Immunization

define "CompletedImmunization":
  exists(GetFinalDose) or exists(GetSingleDose)

define "GetFinalDose":
  Immunization I
    where I.protocolApplied.doseNumber = I.protocolApplied.seriesDoses

define "GetSingleDose":
  Immunization I
    where exists(I.protocolApplied.doseNumber)
      and not exists(I.protocolApplied.seriesDoses)