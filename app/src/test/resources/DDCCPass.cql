library TestArtifact version '1'

using FHIR version '4.0.0'

include "FHIRHelpers" version '4.0.0' called FHIRHelpers

context Immunization

define "CompletedImmunization":
  exists(CompletedImmunizations)

define "CompletedImmunizations":
  Immunization I
    where (I.protocolApplied.doseNumber = I.protocolApplied.seriesDoses)
