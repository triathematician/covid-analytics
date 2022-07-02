/*-
 * #%L
 * coda-data-0.4.0-SNAPSHOT
 * --
 * Copyright (C) 2020 - 2022 Elisha Peterson
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package tri.util.csv

import org.junit.Test
import tri.util.csv.CsvLineSplitter.reconstitute
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CsvUtilsTest {

    @Test
    fun testOneMultilineCsv() {
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("a"))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches(",,a"))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches(",,a,,,,"))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("a,b"))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("a,b, c"))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("a,b, c,"))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("a,b, c,,d"))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,b, c,,"e""""))

        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,"b c d, e f g""""))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,"b c d,
e f g""""))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,"b c d ""e"" f g""""))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches(""""[a, b, c, d, e]""""))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches(""""[a, b, c, d, e]",f,g"""))

        assertFalse(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,""""))
        assertFalse(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches("""a,b,c,""""))

        val x = """xx,ST,q,344,,3Xet,U,44,A C,XB,Region 9,2020-07-06,"""
        val x2 = """"[q, d, c, u, c, ww-19, p-29, t-39, y-49, u-59, i-69, o-79, p+, a, s, d-19, d-29, f-39, g-49, h-59, j-69, k-79, l+, z, x, c, v, b, n, m, q, w, e, r, t, y, u, i, o, p, a, s, d, e, f, g, h, j, k, l, z, x, g, g, g, g, g, g, g, g, g, g, g, g, g, g, g, g]",false,false"""
        val x3 = """xx,ST,q,33,,3 X St,,44,Ex B,XB,Region 9,2020-07-06,[w],1.0,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,0,0,,,10,,500,,,,,15,,1,,1,,14,0,,,,,0,0,0,,0,,,,0,0,0,0,0,0,0,0,0,0,0,,,,,,,,,,,,,,0.1,"[a, b, c, d, e, f-19, g-29, h-39, g-49, g-59, g-69, g-79, g+, g, g, g-19, g-29, g-39, g-49, g-59, g-69, g-79, r+, g, g, g, g, g, g, g, g, g, g, g, r, r, x, x, x, x, x, x, x, x, x, x, x, x, x, x, r, s, s, s, x, x, x, s, s, s, s, s, s, s, s, s, s, s]",false,false,,55,"Sparta",,false,false,[dd]"""
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches(x))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches(x2))
        assertTrue(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches(x3))

        val x4 = """qq,ST,222,1111,111,pob,asdf,44,sdf sfd,JCJC,Region 10,2020-10-07,[3c],1.0,31+,31+,,31+,31+,31+,4-6,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes,No,false,true,true,,true,false,true,true,true,true,true,"[g, g, g, g, x]",[p],"[H C"""
        assertFalse(CsvLineSplitter.MATCH_ONE_MULTILINE_CSV_RECORD.matches(x4))
    }

    @Test
    fun testReconstitute() {
        val test = """a,b,"c
d
e",f"""
        val list = test.splitToSequence('\n').reconstitute()
        assertEquals(test, list.first())
    }

}
