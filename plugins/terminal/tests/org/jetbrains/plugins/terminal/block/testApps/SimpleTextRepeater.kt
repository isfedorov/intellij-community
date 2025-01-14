// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.terminal.block.testApps

import org.jetbrains.plugins.terminal.exp.util.TerminalSessionTestUtil

object SimpleTextRepeater {

  @JvmStatic
  fun main(arg: Array<String>) {
    check(arg.size % 4 == 0) { "Even argument list size is expected, got " + arg.size }
    val items: List<Item> = (0 until arg.size / 4).map {
      Item.fromCommandline(arg.slice(it * 4 until (it + 1) * 4))
    }
    for (item in items) {
      for (i in 1..item.count) {
        print(item.getTextToPrint(i))
      }
    }
  }

  class Item(private val prefix: String, private val withIncrementingId: Boolean, private val withNewLine: Boolean, val count: Int) {
    fun getTextToPrint(id: Int): String {
      return prefix + (if (withIncrementingId) id.toString() else "") + (if (withNewLine) System.lineSeparator() else "")
    }

    fun toCommandline(): List<String> {
      return listOf(prefix, withIncrementingId.toString(), withNewLine.toString(), count.toString())
    }

    companion object {
      val NEW_LINE = Item("", false, true, 1)

      fun fromCommandline(arg: List<String>): Item {
        return Item(arg[0], arg[1].toBooleanStrict(), arg[2].toBooleanStrict(), arg[3].toInt())
      }
    }
  }

  object Helper {
    fun generateCommandLine(items: List<Item>): String {
      val args: Array<String> = items.flatMap { it.toCommandline() }.toTypedArray()
      return TerminalSessionTestUtil.getJavaShellCommand(SimpleTextRepeater::class.java, *args)
    }

    fun getExpectedOutput(items: List<Item>): String {
      return items.flatMap { item ->
        (1..item.count).map { item.getTextToPrint(it) }
      }.joinToString("")
    }
  }
}