package com.github.lburgazzoli.gradle.plugin.karaf.features

import groovy.xml.MarkupBuilder
import java.io.StringWriter
import java.io.Writer

/**
 * @author lburgazzoli
 */
class KarafFeaturesBuilder @JvmOverloads constructor(val writer: Writer = StringWriter()) :
    MarkupBuilder(writer) {
    init {
        super.setOmitNullAttributes(true)
        super.setDoubleQuotes(true)
    }

    override fun toString(): String = writer.toString()
}
