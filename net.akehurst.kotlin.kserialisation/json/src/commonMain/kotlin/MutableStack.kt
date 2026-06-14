/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.kotlin.json

internal fun <T> mutableStackOf(vararg elements: T): MutableStack<T> {
    val stack = MutableStack<T>()
    elements.forEach {
        stack.push(it)
    }
    return stack
}

internal class MutableStack<T>() {
    private val list = mutableListOf<T>()

    val size: Int get() = this.list.size
    val isEmpty: Boolean get() = this.list.size == 0
    val isNotEmpty: Boolean get() = this.list.size != 0
    val elements: List<T> get() = this.list

    fun push(item: T) {
        list.add(item)
    }

    fun peek(): T = list.last()
    fun pop(): T = list.removeAt(list.size - 1)

}