/*
 * Copyright 1999-2017 Alibaba Group.
 *
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
 */
package com.alibaba.p3c.idea.inspection.dc

import com.alibaba.p3c.idea.i18n.P3cBundle
import com.alibaba.p3c.idea.inspection.AliBaseInspection
import com.alibaba.p3c.idea.quickfix.DecorateInspectionGadgetsFix
import com.alibaba.p3c.idea.util.HighlightDisplayLevels
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.siyeh.InspectionGadgetsBundle
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Batch QuickFix Supported
 * @author caikang
 * @date 2016/12/08
 */
class DcLogNonBuiltInStringFormattingInspection : BaseInspection, AliBaseInspection {
    private val messageKey = "com.alibaba.p3c.idea.inspection.dc.LogNonBuiltInStringFormattingInspection"

    constructor()
    /**
     * For Javassist
     */
    constructor(any: Any?) : this()

    /*   init {
           ignoreAnonymousClassMethods = false
           ignoreObjectMethods = false
       }*/

    override fun getDisplayName(): String = P3cBundle.getMessage("$messageKey.message")

    override fun getStaticDescription(): String? = P3cBundle.getMessage("$messageKey.desc")

    override fun ruleName(): String = "LogNonBuiltInStringFormattingRule"

    override fun buildErrorString(vararg infos: Any): String = P3cBundle.message("$messageKey.errMsg",infos)

    override fun createOptionsPanel(): JComponent? = null

    override fun buildFixes(vararg infos: Any?): Array<InspectionGadgetsFix> {
        var fixs = arrayOf<InspectionGadgetsFix>();
        fixs = fixs.plus(DecorateInspectionGadgetsFix(DcUseBuiltInString(),
                P3cBundle.getMessage("$messageKey.UseBuildIn.fix")))
        fixs = fixs.plus(DecorateInspectionGadgetsFix(DcUseEnabledString(),
                P3cBundle.getMessage("$messageKey.UseEnabled.fix")))

        return fixs;
    }

    override fun manualBuildFix(psiElement: PsiElement, isOnTheFly: Boolean): LocalQuickFix? = buildFix(psiElement)

    override fun getDefaultLevel(): HighlightDisplayLevel = HighlightDisplayLevels.MAJOR

    override fun buildVisitor(): BaseInspectionVisitor = LogNonBuiltInStringFormatting()

    private inner class LogNonBuiltInStringFormatting : BaseInspectionVisitor() {

        override fun visitExpressionStatement(statement: PsiExpressionStatement) {
            if((statement.text.contains(".debug")
                    ||statement.text.contains(".info")
                    ||statement.text.contains(".warn")
                    ||statement.text.contains(".trace")
                    ||statement.text.contains(".error"))
                    && statement.text.contains("+")){
                var methodCall = PsiTreeUtil.getChildOfType(statement,PsiMethodCallExpression::class.java) as PsiMethodCallExpression ?:return
                var loggerName = methodCall.methodExpression.qualifierExpression?.text?:return
                if(PsiUtil.resolveClassInType(methodCall.methodExpression.qualifierExpression?.type)?.
                                qualifiedName?.startsWith("org.slf4j.Logger") ?: false){
                    var isError = true;
                    methodCall.argumentList.expressionTypes.forEach {
                        if(it is PsiPolyadicExpression){
                            if(it.children.filter { it is PsiLiteralExpression || it is PsiJavaToken || it is PsiWhiteSpace }.size==0){
                                isError = false
                            }
                        }
                    }
                    if(methodCall.argumentList.expressions.size == 0){
                        return
                    }else if(methodCall.argumentList.expressions.size == 1){
                        if(methodCall.argumentList.expressions[0] is PsiLiteralExpression)return
                    }
                    var plusToken = false;
                    methodCall.argumentList.expressions.forEach {
                        if(it.children.filter { it is PsiJavaToken && it.text.equals("+") }.size!=0){
                            plusToken = true;
                        }
                    }
                    if(!plusToken) return;
                    var expressions = methodCall.argumentList.expressionTypes;
                    var lastVal = expressions[expressions.size-1];
                    if(lastVal !=null && lastVal.isConvertibleFrom(
                                    PsiType.getTypeByName("java.lang.Throwable",statement.project,statement.resolveScope))){
                        isError = false
                    }
                    if(isError) {
                        var ifStatement = PsiTreeUtil.findFirstParent(statement, Condition<PsiElement> {
                            it is PsiIfStatement
                        })
                        if(ifStatement != null){
                            ifStatement.children.filter { it is PsiBinaryExpression||it is PsiMethodCallExpression }
                                    .forEach {
                                        if(it.text.contains(loggerName+".") &&
                                                (it.text.contains("isTraceEnabled")
                                                        ||it.text.contains("isDebugEnabled")
                                                        ||it.text.contains("isInfoEnabled")
                                                        ||it.text.contains("isWarnEnabled")
                                                        ||it.text.contains("isErrorEnabled"))){
                                            isError = false;
                                        }
                                    }
                        }
                    }
                    if(isError){
                        registerError(statement)
                    }
                }
            }
        }


    }

    class DcUseBuiltInString : InspectionGadgetsFix() {
        private val messageKey = "com.alibaba.p3c.idea.inspection.dc.LogNonBuiltInStringFormattingInspection.UseBuildIn.fix"

        @Nls
        override fun getName(): String {
            return InspectionGadgetsBundle.message(messageKey, *arrayOfNulls(0))
        }

        override fun getFamilyName(): String {
            return this.name
        }

        override fun doFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor?.psiElement?: return
            var psiExpressionStatement = descriptor.psiElement as PsiExpressionStatement?: return
            //拼接参数
            var exp = psiExpressionStatement.expression as PsiMethodCallExpression?:return
            if(exp.argumentList.expressions.size>1) return
            var expressions = exp.argumentList.expressions;
            var plusString = expressions[0];
            var newText:String =""
            if(plusString is PsiBinaryExpression){
                plusString.rOperand?:return
                if(!(plusString.lOperand is PsiLiteralExpression)) return;
                var leftText = plusString.lOperand.text;
                if(leftText.endsWith("\"")) {
                    newText = exp.methodExpression.text + "(" + leftText.substring(0, leftText.length - 1) + "{}\"," + plusString.rOperand?.text + ");";
                }else {
                    newText = exp.methodExpression.text + "(" + leftText+ "{}\"," + plusString.rOperand?.text + ");";
                }
            }else if(plusString is PsiPolyadicExpression){
                var firstText = exp.methodExpression.text+"(";
                var args = plusString.children.filter { !(it is PsiWhiteSpace || it is PsiJavaToken) }
                if(!(args[0] is PsiLiteralExpression)) return;
                var i = 0;
                firstText+=args[0].text
                if(firstText.endsWith("\"")) {
                    firstText = firstText.substring(0, firstText.length - 1)
                }
                var paraText = "";
                for (arg in args) {
                    if(i!= 0){
                        firstText = firstText+"{}"
                        paraText = paraText+  ","+arg.text
                    }
                    i++;
                }
                newText = firstText+"\""+paraText+");";
            }
            val psiFacade = JavaPsiFacade.getInstance(project)
            val factory = psiFacade.elementFactory
            var newState = factory.createStatementFromText(newText,psiExpressionStatement.parent);
            psiExpressionStatement.parent.addBefore(newState,psiExpressionStatement);
            psiExpressionStatement.parent.deleteChildRange(psiExpressionStatement,psiExpressionStatement);
        }
    }

    class DcUseEnabledString : InspectionGadgetsFix() {
        private val messageKey = "com.alibaba.p3c.idea.inspection.dc.LogNonBuiltInStringFormattingInspection.UseEnabled.fix"

        @Nls
        override fun getName(): String {
            return InspectionGadgetsBundle.message(messageKey, *arrayOfNulls(0))
        }

        override fun getFamilyName(): String {
            return this.name
        }

        override fun doFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor?.psiElement?: return
            var psiExpressionStatement = descriptor.psiElement as PsiExpressionStatement?: return
            var methodCall = PsiTreeUtil.getChildOfType(psiExpressionStatement,PsiMethodCallExpression::class.java) as PsiMethodCallExpression ?:return
            var loggerText = "if("+methodCall.methodExpression.qualifierExpression?.text?:return
            if(psiExpressionStatement.text.contains(".debug")){
                loggerText = loggerText+".isDebugEnabled())"
            }else if(psiExpressionStatement.text.contains(".info")){
                loggerText = loggerText+".isInfoEnabled())"
            }else if(psiExpressionStatement.text.contains(".warn")){
                loggerText = loggerText+".isWarnEnabled())"
            }else if(psiExpressionStatement.text.contains(".trace")){
                loggerText = loggerText+".isTraceEnabled())"
            }else if(psiExpressionStatement.text.contains(".error")){
                loggerText = loggerText+".isErrorEnabled())"
            }
            val psiFacade = JavaPsiFacade.getInstance(project)
            val factory = psiFacade.elementFactory
            var ifStatement = factory.createStatementFromText(loggerText+"{\n"+psiExpressionStatement.text+"\n}\n",psiExpressionStatement.parent);
            psiExpressionStatement.parent.addBefore(ifStatement,psiExpressionStatement);
            psiExpressionStatement.parent.deleteChildRange(psiExpressionStatement,psiExpressionStatement);
        }
    }
}
