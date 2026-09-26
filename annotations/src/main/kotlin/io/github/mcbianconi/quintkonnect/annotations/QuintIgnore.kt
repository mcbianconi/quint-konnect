package io.github.mcbianconi.quintkonnect.annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Marks a property of a `@Serializable` state class as excluded from `TypedState.check`'s
 * comparison and field-level diff (`core` module): a mismatch on this field is never reported and
 * never fails the check.
 *
 * The property still decodes normally through the configured serializer: if the spec's ITF value
 * doesn't have this field at all, it needs a default value (or to be nullable), same as any other
 * field absent from the spec.
 *
 * Visible at runtime via [kotlinx.serialization.descriptors.SerialDescriptor.getElementAnnotations],
 * which requires the [SerialInfo] meta-annotation.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class QuintIgnore
