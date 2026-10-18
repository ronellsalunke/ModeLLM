package dev.ronellsalunke.modellm.data.mapper

import dev.ronellsalunke.modellm.data.local.entity.CanonicalModelEntity
import dev.ronellsalunke.modellm.data.local.entity.CanonicalSearchEntity
import dev.ronellsalunke.modellm.data.local.entity.LabEntity
import dev.ronellsalunke.modellm.data.model.CanonicalModel
import dev.ronellsalunke.modellm.data.model.Lab
import dev.ronellsalunke.modellm.data.model.ModelLimits
import dev.ronellsalunke.modellm.data.model.Modality

fun CanonicalModelEntity.toSearchEntity(rowId: Long): CanonicalSearchEntity =
    CanonicalSearchEntity(
        rowId = rowId,
        canonicalId = canonicalId,
        name = name,
        labName = labName,
        family = family.orEmpty(),
        description = description.orEmpty(),
        providerNames = providerNames,
    )

fun CanonicalModelEntity.toDomain(): CanonicalModel = CanonicalModel(
    id = canonicalId,
    name = name,
    description = description,
    family = family,
    labId = labId,
    labName = labName,
    providerCount = providerCount,
    attachment = attachment,
    reasoning = reasoning,
    toolCall = toolCall,
    structuredOutput = structuredOutput,
    temperature = temperature,
    openWeights = openWeights,
    releaseDate = releaseDate,
    lastUpdated = lastUpdated,
    knowledge = knowledge,
    limits = ModelLimits(context = contextLimit, input = null, output = maxOutputTokens),
    minInputCost = minInputCost,
    minOutputCost = minOutputCost,
    inputModalities = buildList {
        if (supportsTextInput) add(Modality.Text)
        if (supportsImageInput) add(Modality.Image)
        if (supportsPdfInput) add(Modality.Pdf)
        if (supportsAudioInput) add(Modality.Audio)
        if (supportsVideoInput) add(Modality.Video)
    },
    outputModalities = buildList {
        if (supportsTextOutput) add(Modality.Text)
        if (supportsImageOutput) add(Modality.Image)
        if (supportsPdfOutput) add(Modality.Pdf)
        if (supportsAudioOutput) add(Modality.Audio)
        if (supportsVideoOutput) add(Modality.Video)
    },
    modelType = modelType,
)

fun LabEntity.toDomain(): Lab = Lab(
    id = id,
    name = name,
    description = description,
    modelCount = modelCount,
    providerCount = providerCount,
    lastUpdated = lastUpdated,
)
