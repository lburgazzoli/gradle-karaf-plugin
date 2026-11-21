package com.github.lburgazzoli.gradle.plugin.karaf.features.model

import javax.inject.Inject

/**
 * @author lburgazzoli
 */
abstract class FeatureCondition @Inject constructor() : FeatureDefinition() {
    var condition: String? = null
}
