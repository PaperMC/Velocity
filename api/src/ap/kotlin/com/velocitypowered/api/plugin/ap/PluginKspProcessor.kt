/*
 * Copyright (C) 2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.plugin.ap

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.validate
import com.velocitypowered.api.plugin.ap.PluginProcessingEnvironment.AnnotationWrapper
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter

class PluginKspProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {
  private val pluginEnv: PluginProcessingEnvironment<KSAnnotated> =
      KspPluginEnvironment(environment.logger, environment.codeGenerator)

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver.getSymbolsWithAnnotation(PluginProcessingEnvironment.PLUGIN_CLASS_NAME)
    // only generate a plugin meta file once all symbols are resolved
    // this way any potential generated constants used in the plugin annotation are available
    val remaining = symbols.filter { !it.validate() }

    for (symbol in symbols.filter { it.validate() }) {
      if (!pluginEnv.process(symbol)) {
        break
      }
    }

    return remaining.toList()
  }

  class Provider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
      return PluginKspProcessor(environment)
    }
  }

  internal class KspPluginEnvironment(
      private val logger: KSPLogger,
      private val filer: CodeGenerator
  ) : PluginProcessingEnvironment<KSAnnotated>() {
    override fun logNotice(message: String, element: KSAnnotated) {
      logger.logging(message, element)
    }

    override fun logWarning(message: String, element: KSAnnotated) {
      logger.warn(message, element)
    }

    override fun logError(message: String, element: KSAnnotated) {
      logger.error(message, element)
    }

    override fun logError(message: String, element: KSAnnotated, ex: Exception?) {
      logger.error(message, element)
      if (ex != null) {
        logger.exception(ex)
      }
    }

    override fun isClass(element: KSAnnotated?): Boolean {
      return element is KSClassDeclaration
    }

    override fun getQualifiedName(element: KSAnnotated): String {
      return ((element as KSClassDeclaration).qualifiedName ?: element.simpleName).asString()
    }

    override fun getPluginAnnotation(element: KSAnnotated): AnnotationWrapper {
      val annotation =
          element.annotations.find {
            it.shortName.asString() == "Plugin" &&
                it.annotationType.resolve().declaration.qualifiedName!!.asString() ==
                    PLUGIN_CLASS_NAME
          }
              ?: throw IllegalStateException(
                  "The provided element $element does not have a @Plugin annotation"
              )

      return KspAnnotationWrapper(annotation)
    }

    override fun openWriter(pkg: String, name: String, sourceElement: KSAnnotated): BufferedWriter {
      val file =
          if (sourceElement is KSDeclaration) {
            sourceElement.containingFile
          } else {
            null
          }

      val extIdx = name.lastIndexOf('.')
      if (extIdx == -1) {
        throw IOException("File name $name did not have an extension!")
      }
      val fileName = name.substring(0, extIdx)
      val extension = name.substring(extIdx + 1, name.length)

      val output =
          filer.createNewFile(
              Dependencies(aggregating = false, sources = file?.let { arrayOf(it) } ?: arrayOf()),
              pkg,
              fileName,
              extension
          )
      return BufferedWriter(OutputStreamWriter(output))
    }
  }

  internal class KspAnnotationWrapper(anno: KSAnnotation) : AnnotationWrapper {
    private val explicit: Map<String, Any?>
    private val default: Map<String, Any?>

    init {
      val explicit = anno.arguments.associate { it.name!!.asString() to it.value }
      this.explicit = explicit
      default =
          anno.defaultArguments
              .filter { it.name!!.asString() !in explicit }
              .associate { it.name!!.asString() to it.value }
    }

    private fun unbox(arg: Any?): Any? {
      return when (arg) {
        is KSTypeReference -> arg // TODO: not used for Velocity
        is KSPropertyDeclaration -> arg.simpleName.asString()
        is KSAnnotation -> KspAnnotationWrapper(arg)
        is List<*> -> {
          val unboxed = mutableListOf<Any>()
          for (o in arg) {
            unboxed.add(this.unbox(o)!!)
          }
          unboxed
        }
        else -> arg
      }
    }

    private fun <T : Any> unboxAndCast(o: Any, expectedType: Class<T>): T {
      val value = unbox(o)
      require(expectedType.isInstance(value)) {
        ("Expected '$value' to be a $expectedType, but it was not")
      }

      return expectedType.cast(value)
    }

    override fun <T : Any> get(key: String, expectedType: Class<T>): T? {
      val value: Any = this.explicit[key] ?: this.default[key] ?: return null

      return unboxAndCast(value, expectedType)
    }

    override fun <T : Any> getList(key: String?, expectedType: Class<T>): List<T>? {
      val value: Any = this.explicit[key] ?: this.default[key] ?: return null

      return if (value is List<*>) {
        value.map { unboxAndCast(it!!, expectedType) }
      } else {
        listOf(unboxAndCast(value, expectedType))
      }
    }

    override fun isExplicit(key: String?): Boolean {
      return key in explicit
    }
  }
}
