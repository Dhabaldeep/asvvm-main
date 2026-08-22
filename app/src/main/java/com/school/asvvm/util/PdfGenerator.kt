package com.school.asvvm.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.school.asvvm.data.model.Mark
import com.school.asvvm.data.model.Student
import com.school.asvvm.data.model.getSubjectsForClass

object PdfGenerator {

    /**
     * Main function to trigger the Android Print Service
     */
    fun generateAndPrint(
        context: Context,
        student: Student,
        marks: List<Mark>,
        subjectConfigs: List<com.school.asvvm.data.model.SubjectConfig>,
        total: String,
        percentage: String,
        grade: String
    ) {
        val webView = WebView(context)
        val htmlContent = buildHtmlReportCard(student, marks, subjectConfigs, total, percentage, grade)
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("ReportCard_${student.name}")
                val jobName = "ASVVM_ReportCard_${student.id}"
                
                printManager.print(jobName, printAdapter, PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .build())
            }
        }
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    /**
     * Internal function to generate the HTML string with CSS styling
     */
    private fun buildHtmlReportCard(
        student: Student,
        marks: List<Mark>,
        subjectConfigs: List<com.school.asvvm.data.model.SubjectConfig>,
        total: String,
        percentage: String,
        grade: String
    ): String {
        val uniqueSubjects = subjectConfigs.map { it.subjectName }.distinct()
        
        // Determine completed terms from marks
        val completedTerms = marks.map { it.term }.toSet()
        val isAnnualCompleted = completedTerms.contains("ANNUAL")
        val isSecondHalfCompleted = completedTerms.contains("SECOND_HALF")
        val isFirstHalfCompleted = completedTerms.contains("FIRST_HALF")

        val resultStatus = when {
            isAnnualCompleted -> if (grade == "F") "DETAINED" else "PROMOTED"
            isSecondHalfCompleted -> if (grade == "F") "NEEDS IMPROVEMENT" else "2ND TERM COMPLETED"
            isFirstHalfCompleted -> if (grade == "F") "NEEDS IMPROVEMENT" else "1ST TERM COMPLETED"
            else -> "RESULT AWAITED"
        }

        // Calculate dynamic total max marks for completed terms
        var dynamicMaxTotal = 0
        var dynamicObtainedTotal = 0

        // Generate Table Rows dynamically based on subjects
        val rows = uniqueSubjects.joinToString("") { subject ->
            val m1 = marks.find { it.subject == subject && it.term == "FIRST_HALF" }
            val m2 = marks.find { it.subject == subject && it.term == "SECOND_HALF" }
            val ma = marks.find { it.subject == subject && it.term == "ANNUAL" }
            
            fun getW(m: Mark?) = m?.writtenMarks ?: "-"
            fun getO(m: Mark?, hasOral: Boolean) = if(hasOral) (m?.oralMarks ?: "-") else "-"
            fun getT(m: Mark?) = (m?.writtenMarks?.toIntOrNull() ?: 0) + (m?.oralMarks?.toIntOrNull() ?: 0)
            
            val grandTotal = getT(m1) + getT(m2) + getT(ma)

            val sConfigs = subjectConfigs.filter { it.subjectName == subject }
            val c1 = sConfigs.find { it.term == "FIRST_HALF" }
            val c2 = sConfigs.find { it.term == "SECOND_HALF" }
            val ca = sConfigs.find { it.term == "ANNUAL" }

            val o1 = getO(m1, c1?.hasOral == true)
            val o2 = getO(m2, c2?.hasOral == true)
            val oa = getO(ma, ca?.hasOral == true)

            // Add to dynamic totals for completed terms
            if (m1 != null && c1 != null) dynamicMaxTotal += c1.maxWritten + (if (c1.hasOral) c1.maxOral else 0)
            if (m2 != null && c2 != null) dynamicMaxTotal += c2.maxWritten + (if (c2.hasOral) c2.maxOral else 0)
            if (ma != null && ca != null) dynamicMaxTotal += ca.maxWritten + (if (ca.hasOral) ca.maxOral else 0)
            dynamicObtainedTotal += grandTotal

            """
            <tr>
                <td class="subject-name">$subject</td>
                <td>${getW(m1)}</td><td class="oral-col">${o1}</td>
                <td>${getW(m2)}</td><td class="oral-col">${o2}</td>
                <td>${getW(ma)}</td><td class="oral-col">${oa}</td>
                <td class="grand-total-cell">$grandTotal</td>
            </tr>
            """
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    @page { margin: 0; }
                    body { 
                        font-family: 'Segoe UI', Roboto, Arial, sans-serif; 
                        margin: 0; padding: 35px; background: #fff; color: #111;
                    }
                    .outer-border {
                        border: 4px double #1A237E;
                        padding: 20px;
                        height: 93%;
                        position: relative;
                        box-sizing: border-box;
                    }
                    .header { text-align: center; margin-bottom: 20px; }
                    .school-name { 
                        font-size: 26px; font-weight: 900; color: #1A237E; 
                        margin: 0; text-transform: uppercase;
                    }
                    .school-sub { font-size: 13px; color: #444; margin: 4px 0; font-weight: bold; }
                    .report-tag { 
                        display: inline-block; background: #1A237E; color: white;
                        padding: 4px 18px; border-radius: 4px; margin-top: 8px;
                        font-size: 14px; font-weight: bold;
                    }
                    .student-info { 
                        margin: 20px 0; border: 1px solid #333;
                        display: grid; grid-template-columns: 1fr 1fr;
                        padding: 12px; background: #f9f9f9;
                    }
                    .info-box { font-size: 13px; line-height: 1.8; }
                    .label { font-weight: bold; color: #1A237E; width: 100px; display: inline-block; }
                    
                    table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                    th { 
                        background-color: #1A237E; color: white; padding: 6px 2px; 
                        font-size: 11px; border: 1px solid #000;
                    }
                    td { border: 1px solid #000; padding: 8px 2px; text-align: center; font-size: 12px; }
                    .subject-name { text-align: left; padding-left: 10px; font-weight: bold; background: #f2f2f2; }
                    .oral-col { background: #fafafa; }
                    .grand-total-cell { font-weight: bold; background: #e8eaf6; font-size: 14px; }

                    .result-summary {
                        margin-top: 25px; display: flex; 
                        justify-content: space-around; align-items: center;
                        border: 2px solid #1A237E; padding: 12px; background: #fdfdfd;
                    }
                    .res-item { text-align: center; }
                    .res-val { font-size: 18px; font-weight: bold; color: #1A237E; display: block; }
                    .res-lbl { font-size: 10px; text-transform: uppercase; color: #666; font-weight: bold; }

                    .grading-note {
                        margin-top: 15px; font-size: 9px; color: #555; text-align: center;
                        font-style: italic;
                    }

                    .signatures { 
                        margin-top: 70px; display: flex; justify-content: space-between; 
                    }
                    .sig-line { border-top: 1.5px solid #333; width: 150px; text-align: center; padding-top: 6px; font-weight: bold; font-size: 11px; }
                </style>
            </head>
            <body>
                <div class="outer-border">
                    
                    <div class="header">
                        <div class="school-name">Aralbanshi Sishu Vikash Vidya Mondir</div>
                        <div class="school-sub">Primary Academic Session: 2026-27</div>
                        <div class="report-tag">STUDENT PROGRESS REPORT</div>
                    </div>

                    <div class="student-info">
                        <div class="info-box"><span class="label">Student Name:</span> ${student.name}</div>
                        <div class="info-box"><span class="info-label">Roll Number:</span> ${student.rollNo}</div>
                        <div class="info-box"><span class="info-label">Class:</span> ${student.className}</div>
                        <div class="info-box"><span class="info-label">Guardian:</span> ${student.guardian}</div>
                    </div>

                    <table>
                        <thead>
                            <tr>
                                <th rowspan="2" style="width: 25%">SUBJECTS</th>
                                <th colspan="2">1st Term</th>
                                <th colspan="2">2nd Term</th>
                                <th colspan="2">Annual</th>
                                <th rowspan="2" style="width: 12%">Grand<br>Total</th>
                            </tr>
                            <tr>
                                <th style="width: 9%">W</th><th style="width: 7%">O</th>
                                <th style="width: 9%">W</th><th style="width: 7%">O</th>
                                <th style="width: 9%">W</th><th style="width: 7%">O</th>
                            </tr>
                        </thead>
                        <tbody>
                            $rows
                        </tbody>
                    </table>

                    <div class="result-summary">
                        <div class="res-item">
                            <span class="res-lbl">Marks Obtained</span>
                            <span class="res-val">$total</span>
                        </div>
                        <div class="res-item">
                            <span class="res-lbl">Percentage</span>
                            <span class="res-val">$percentage%</span>
                        </div>
                        <div class="res-item">
                            <span class="res-lbl">Grade</span>
                            <span class="res-val">$grade</span>
                        </div>
                        <div class="res-item">
                            <span class="res-lbl">Final Status</span>
                            <span class="res-val" style="color: ${if(grade == "F") "#D32F2F" else "#2E7D32"}">
                                $resultStatus
                            </span>
                        </div>
                    </div>

                    <div class="grading-note">
                        W: Written Examination | O: Oral/Project Evaluation. Grade Scale: A+(90-100), A(80-89), B+(70-79), B(60-69), C(45-59), P(35-44), F(Below 35)
                    </div>

                    <div class="signatures">
                        <div class="sig-line">Class Teacher</div>
                        <div class="sig-line">Guardian</div>
                        <div class="sig-line">Headmaster</div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}