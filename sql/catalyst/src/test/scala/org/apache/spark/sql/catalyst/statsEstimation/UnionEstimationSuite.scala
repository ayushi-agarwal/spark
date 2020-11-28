/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.catalyst.statsEstimation

import org.apache.spark.sql.catalyst.expressions.{AttributeMap, AttributeReference}
import org.apache.spark.sql.catalyst.plans.logical
import org.apache.spark.sql.catalyst.plans.logical.{ColumnStat, Union}
import org.apache.spark.sql.types.{ByteType, Decimal, DecimalType, DoubleType, FloatType, IntegerType, LongType, ShortType}

class UnionEstimationSuite extends StatsEstimationTestBase {

  test("test row size estimation") {
    val attrInt = AttributeReference("cint", IntegerType)()

    val sz: Option[BigInt] = Some(1024)
    val child1 = StatsTestPlan(
      outputList = Seq(attrInt),
      rowCount = 2,
      attributeStats = AttributeMap(Nil),
      size = sz)

    val child2 = StatsTestPlan(
      outputList = Seq(attrInt),
      rowCount = 2,
      attributeStats = AttributeMap(Nil),
      size = sz)

    val union = Union(Seq(child1, child2))
    val expectedStats = logical.Statistics(sizeInBytes = 2 * 1024, rowCount = Some(4))
    assert(union.stats === expectedStats)
  }

  test("col stats estimation") {
    val sz: Option[BigInt] = Some(1024)

    val attrInt = AttributeReference("cint", IntegerType)()
    val attrDouble = AttributeReference("cdouble", DoubleType)()
    val attrShort = AttributeReference("cshort", ShortType)()
    val attrLong = AttributeReference("clong", LongType)()
    val attrByte = AttributeReference("cbyte", ByteType)()
    val attrFloat = AttributeReference("cfloat", FloatType)()
    val attrDecimal = AttributeReference("cdecimal", DecimalType.SYSTEM_DEFAULT)()

    val s1: Short = 1
    val s2: Short = 4
    val b1: Byte = 1
    val b2: Byte = 4
    val columnInfo: AttributeMap[ColumnStat] = AttributeMap(
      Seq(
        attrInt -> ColumnStat(
          distinctCount = Some(2),
          min = Some(1),
          max = Some(4),
          nullCount = Some(0),
          avgLen = Some(4),
          maxLen = Some(4)),
        attrDouble -> ColumnStat(
          distinctCount = Some(2),
          min = Some(5.0),
          max = Some(4.0),
          nullCount = Some(0),
          avgLen = Some(4),
          maxLen = Some(4)),
        attrShort -> ColumnStat(min = Some(s1), max = Some(s2)),
        attrLong -> ColumnStat(min = Some(1L), max = Some(4L)),
        attrByte -> ColumnStat(min = Some(b1), max = Some(b2)),
        attrFloat -> ColumnStat(min = Some(1.1f), max = Some(4.1f)),
        attrDecimal -> ColumnStat(min = Some(Decimal(13.5)), max = Some(Decimal(19.5)))))

    val s3: Short = 2
    val s4: Short = 6
    val b3: Byte = 2
    val b4: Byte = 6
    val columnInfo1: AttributeMap[ColumnStat] = AttributeMap(
      Seq(
        AttributeReference("cint1", IntegerType)() -> ColumnStat(
          distinctCount = Some(2),
          min = Some(3),
          max = Some(6),
          nullCount = Some(0),
          avgLen = Some(8),
          maxLen = Some(8)),
        AttributeReference("cdouble1", DoubleType)() -> ColumnStat(
          distinctCount = Some(2),
          min = Some(2.0),
          max = Some(7.0),
          nullCount = Some(0),
          avgLen = Some(8),
          maxLen = Some(8)),
        AttributeReference("cshort1", ShortType)() -> ColumnStat(min = Some(s3), max = Some(s4)),
        AttributeReference("clong1", LongType)() -> ColumnStat(min = Some(2L), max = Some(6L)),
        AttributeReference("cbyte1", ByteType)() -> ColumnStat(min = Some(b3), max = Some(b4)),
        AttributeReference("cfloat1", FloatType)() -> ColumnStat(
          min = Some(2.2f),
          max = Some(6.1f)),
        AttributeReference("cdecimal1", DecimalType.SYSTEM_DEFAULT)() -> ColumnStat(
          min = Some(Decimal(14.5)),
          max = Some(Decimal(19.9)))))

    val child1 = StatsTestPlan(
      outputList = columnInfo.keys.toSeq.sortWith(_.exprId.id < _.exprId.id),
      rowCount = 2,
      attributeStats = columnInfo,
      size = sz)

    val child2 = StatsTestPlan(
      outputList = columnInfo1.keys.toSeq.sortWith(_.exprId.id < _.exprId.id),
      rowCount = 2,
      attributeStats = columnInfo1,
      size = sz)

    val union = Union(Seq(child1, child2))

    val expectedStats = logical.Statistics(
      sizeInBytes = 2 * 1024,
      rowCount = Some(4),
      attributeStats = AttributeMap(
        Seq(
          attrInt -> ColumnStat(min = Some(1), max = Some(6)),
          attrDouble -> ColumnStat(min = Some(2.0), max = Some(7.0)),
          attrShort -> ColumnStat(min = Some(s1), max = Some(s4)),
          attrLong -> ColumnStat(min = Some(1L), max = Some(6L)),
          attrByte -> ColumnStat(min = Some(b1), max = Some(b4)),
          attrFloat -> ColumnStat(min = Some(1.1f), max = Some(6.1f)),
          attrDecimal -> ColumnStat(min = Some(Decimal(13.5)), max = Some(Decimal(19.9))))))
    assert(union.stats === expectedStats)
  }

  test("col stats estimation when min max stats not present for one child") {
    val sz: Option[BigInt] = Some(1024)

    val attrInt = AttributeReference("cint", IntegerType)()

    val columnInfo: AttributeMap[ColumnStat] = AttributeMap(
      Seq(
        attrInt -> ColumnStat(
          distinctCount = Some(2),
          min = Some(2),
          max = Some(2),
          nullCount = Some(0),
          avgLen = Some(4),
          maxLen = Some(4))))

    val columnInfo1: AttributeMap[ColumnStat] = AttributeMap(
      Seq(
        AttributeReference("cint1", IntegerType)() -> ColumnStat(
          distinctCount = Some(2),
          nullCount = Some(0),
          avgLen = Some(8),
          maxLen = Some(8))))

    val child1 = StatsTestPlan(
      outputList = columnInfo.keys.toSeq,
      rowCount = 2,
      attributeStats = columnInfo,
      size = sz)

    val child2 = StatsTestPlan(
      outputList = columnInfo1.keys.toSeq,
      rowCount = 2,
      attributeStats = columnInfo1,
      size = sz)

    val union = Union(Seq(child1, child2))

    val expectedStats = logical.Statistics(sizeInBytes = 2 * 1024, rowCount = Some(4))
    assert(union.stats === expectedStats)
  }
}
