/*
 * SpinalHDL
 * Copyright (c) Dolu, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package spinal

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by PIC18F on 21.08.2014.
 */


object BaseType {

  def assignFrom(baseType: BaseType, initialConsumer: Node, that: Node): Unit = {
    //var consumer: Node = if (baseType.isReg) baseType.inputs(0) else baseType
    var consumer = initialConsumer
    var consumerInputId: Int = 0
    var whenHit = baseType.whenScope == null

    for (when <- when.stack.stack.reverseIterator) {

      if (!whenHit) {
        if (when == baseType.whenScope) whenHit = true
      } else {
        if (!when.autoGeneratedMuxs.contains(consumer.inputs(consumerInputId))) {
          // val mux = Multiplex(when.cond, consumer.inputs(consumerInputId), consumer.inputs(consumerInputId), true)
          val mux = baseType.newMultiplexor(when.cond, consumer.inputs(consumerInputId), consumer.inputs(consumerInputId))
          mux.whenMux = true;
          consumer.inputs(consumerInputId) = mux
          when.autoGeneratedMuxs += mux
        }
        consumer = consumer.inputs(consumerInputId)
        consumerInputId = if (when.isTrue) 1 else 2
      }

    }

    if (!whenHit)
      throw new Exception("Basetype is affected outside his when scope")

    consumer.inputs(consumerInputId) = that

  }
}


abstract class BaseType extends Node with Data with Nameable {
  inputs += null


  val whenScope = when.stack.head()
  var compositeAssign : Assignable = null

  final override def assignFrom(that: Data): Unit = {
    if(compositeAssign != null){
      compositeAssign.assignFrom(that)
    }else{
      assignFromImpl(that)
    }
  }


  override def getBitsWidth : Int = getWidth

  def isReg = inputs(0).isInstanceOf[Reg]

  //  override def :=(bits: this.type): Unit = assignFrom(bits)

  def assignFromImpl(that: Data): Unit = {
    that match {
      case that: BaseType => {
        BaseType.assignFrom(this, this, that)
      }
      case _ => throw new Exception("Undefined assignement")
    }
  }


 // def castThatInSame(that: BaseType): this.type = throw new Exception("Not defined")


  override def flatten: ArrayBuffer[(String, BaseType)] = ArrayBuffer((getName(), this));


  override def clone: this.type = {
    val res = this.getClass.newInstance.asInstanceOf[this.type];
    res.dir = this.dir
    res
  }




  def newMultiplexor(sel: Bool, whenTrue: Node, whenFalse: Node): Multiplexer


  def newLogicalOperator(opName: String, right: Node, normalizeInputsImpl: (Node) => Unit): Bool = {
    val op = BinaryOperator(opName, this, right, WidthInfer.oneWidth, normalizeInputsImpl)
    val typeNode = new Bool()
    typeNode.inputs(0) = op
    typeNode
  }

  def newBinaryOperator(opName: String, right: Node, getWidthImpl: (Node) => Int, normalizeInputsImpl: (Node) => Unit): this.type = {
    val op = BinaryOperator(opName, this, right, getWidthImpl, normalizeInputsImpl)
    val typeNode = addTypeNodeFrom(op)
    typeNode
  }

  def newUnaryOperator(opName: String, getWidthImpl: (Node) => Int = WidthInfer.inputMaxWidthl): this.type = {
    val op = UnaryOperator(opName, this, getWidthImpl, InputNormalize.none)
    val typeNode = addTypeNodeFrom(op)
    typeNode
  }

  def castFrom(opName: String, that: Node, getWidthImpl: (Node) => Int = WidthInfer.inputMaxWidthl): this.type = {
    val op = Cast(opName, that, getWidthImpl)
    this.setInput(op)
    this
  }
  def enumCastFrom(opName: String, that: Node, getWidthImpl: (Node) => Int = WidthInfer.inputMaxWidthl): this.type = {
    val op = EnumCast(this.asInstanceOf[SpinalEnumCraft[_]],opName, that, getWidthImpl)
    this.setInput(op)
    this
  }
  def newFunction(opName: String, args: List[Node], getWidthImpl: (Node) => Int = WidthInfer.inputMaxWidthl): this.type = {
    val op = Function(opName, args, getWidthImpl)
    val typeNode = addTypeNodeFrom(op)
    typeNode
  }

  def newResize(opName: String, args: List[Node], getWidthImpl: (Node) => Int = WidthInfer.inputMaxWidthl): this.type = {
    val op = Resize(opName, args, getWidthImpl)
    val typeNode = addTypeNodeFrom(op)
    typeNode
  }

  def addTypeNodeFrom(node: Node): this.type = {
    val typeNode = this.clone()
    typeNode.setInput(node)
    typeNode
  }


  override def toString() : String = s"${getClassIdentifier}(named ${"\"" + getName() + "\""},into ${if(component == null) "null" else component.getClass.getSimpleName}})"
}
