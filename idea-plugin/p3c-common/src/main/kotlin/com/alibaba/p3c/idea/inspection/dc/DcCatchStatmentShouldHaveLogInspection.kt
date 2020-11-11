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
import com.alibaba.p3c.pmd.lang.java.util.namelist.NameListConfig
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.extapi.psi.PsiElementBase
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Factory
import com.intellij.psi.*
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.impl.java.stubs.JavaParameterListElementType
import com.intellij.psi.util.*
import com.siyeh.InspectionGadgetsBundle
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import net.sourceforge.pmd.lang.java.ast.ASTReferenceType
import org.jetbrains.annotations.Nls
import java.util.*
import javax.swing.JComponent

/**
 * Batch QuickFix Supported
 * @author caikang
 * @date 2016/12/08
 */
class DcCatchStatmentShouldHaveLogInspection : BaseInspection, AliBaseInspection {
    private val messageKey = "com.alibaba.p3c.idea.inspection.dc.CatchStatmentShouldHaveLogInspection"

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

    override fun ruleName(): String = "CatchStatmentShouldHaveLogRule"

    override fun buildErrorString(vararg infos: Any): String = P3cBundle.message("$messageKey.errMsg",infos)

    override fun createOptionsPanel(): JComponent? = null

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        return DecorateInspectionGadgetsFix(DcAddLogOutput().setLevel("error"),
                P3cBundle.getMessage("$messageKey.fix"))
    }

    override fun buildFixes(vararg infos: Any?): Array<InspectionGadgetsFix> {
        var fixs = arrayOf<InspectionGadgetsFix>();
        fixs = fixs.plus(DecorateInspectionGadgetsFix(DcAddLogOutput().setLevel("error"),
                P3cBundle.getMessage("$messageKey.error.fix")))
                .plus(DecorateInspectionGadgetsFix(DcAddLogOutput().setLevel("warn"),
                        P3cBundle.getMessage("$messageKey.warn.fix")))
                .plus(DecorateInspectionGadgetsFix(DcAddLogOutput().setLevel("info"),
                        P3cBundle.getMessage("$messageKey.info.fix")))
                .plus(DecorateInspectionGadgetsFix(DcAddLogOutput().setLevel("debug"),
                        P3cBundle.getMessage("$messageKey.debug.fix")))
        return fixs;
    }

    override fun manualBuildFix(psiElement: PsiElement, isOnTheFly: Boolean): LocalQuickFix? = buildFix(psiElement)

    override fun getDefaultLevel(): HighlightDisplayLevel = HighlightDisplayLevels.MAJOR

    override fun buildVisitor(): BaseInspectionVisitor = CatchStatmentShouldHaveLog()

    private inner class CatchStatmentShouldHaveLog : BaseInspectionVisitor() {
        override fun visitCatchSection(section: PsiCatchSection) {
            section.parameter?: return
            var psiParameter =  section.parameter as PsiParameter;
            section.catchBlock?: return
            var noProcess = true;
            section.catchBlock!!.children.forEach {
                if(it is PsiIfStatement){
                   var blco =  PsiTreeUtil.findChildOfType(it, PsiBlockStatement::class.java,true);
                    blco?.children?.filter {
                        it is PsiCodeBlock
                    }?.forEach {  it.children.forEach {
                        if(!checkElement(it,psiParameter)){
                            noProcess = false;
                            return
                        }
                    }}
                }else{
                    if(!checkElement(it,psiParameter)){
                        noProcess = false;
                    }
                }
            }

            if(noProcess){
                registerError(section)
            }

        }



        fun checkElement(it: PsiElement,psiParameter: PsiParameter): Boolean {
            if(it is PsiExpressionStatement){
                var useException = false;
                var useLogger = false;
                it.children.forEach {
                    if(it is PsiMethodCallExpression){
                        //查询参数引用
                        it.argumentList.expressions.forEach {
                            if (it is PsiReferenceExpression){
                                if(it.isReferenceTo(psiParameter)){
                                    return false;
                                }
                            }else if(it is PsiMethodCallExpression){
                                if(!checkElement(it,psiParameter)){
                                    return false;
                                }
                            }
                        }
                    }
                }
            }else if(it is PsiThrowStatement) {
                it.children.forEach {
                    if (it is PsiMethodCallExpression) {
                        //查询参数引用
                        it.argumentList.expressions.forEach {
                            if (it is PsiReferenceExpression) {
                                if (it.isReferenceTo(psiParameter)) {
                                    return false;
                                }
                            }
                        }
                    } else if (it is PsiReferenceExpression) {
                        if (it.isReferenceTo(psiParameter)) {
                            return false;
                        }
                    } else if (it is PsiNewExpression) {
                        //查询参数引用
                        it.argumentList?.expressions?.forEach {
                            if (it is PsiReferenceExpression) {
                                if (it.isReferenceTo(psiParameter)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }else if(it is PsiMethodCallExpression){
                //查询参数引用
                it.argumentList.expressions.forEach {
                    if (it is PsiReferenceExpression){
                        if(it.isReferenceTo(psiParameter)){
                            return false;
                        }
                    }else if(it is PsiMethodCallExpression){
                        if(!checkElement(it,psiParameter)){
                            return false;
                        }
                    }
                }
            }
            return true;
        }

    }

    class DcAddLogOutput : InspectionGadgetsFix() {

        var level:String="error";

        private val messageKey = "com.alibaba.p3c.idea.inspection.dc.CatchStatmentShouldHaveLogInspection.error.fix"

        fun setLevel(level: String):DcAddLogOutput{
            this.level = level
            return this;
        }

        @Nls
        override fun getName(): String {
            return InspectionGadgetsBundle.message(messageKey, *arrayOfNulls(0))
        }

        override fun getFamilyName(): String {
            return this.name
        }

        override fun doFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor?.psiElement?: return
            var psiCatchSection = descriptor.psiElement as PsiCatchSection?: return
            var psiClass = PsiTreeUtil.findFirstParent(psiCatchSection, Condition<PsiElement>{
                it is PsiClass && (it as PsiClass).containingFile.name.endsWith(".java")
            }) as PsiClass
            val psiFacade = JavaPsiFacade.getInstance(project)
            val factory = psiFacade.elementFactory
            //处理导入
            var implements = PsiTreeUtil.findChildOfType(psiClass.containingFile, PsiImportList::class.java,true)
            if(implements!= null){
                if(implements.findSingleClassImportStatement("org.slf4j.*")==null) {
                    if (implements.findSingleClassImportStatement("org.slf4j.Logger") == null) {
                        var log4Class = psiFacade.findClass("org.slf4j.Logger", implements.resolveScope)
                        if (log4Class != null) {
                            implements.add(factory.createImportStatement(log4Class))
                        }
                    }
                    if (implements.findSingleClassImportStatement("org.slf4j.LoggerFactory") == null) {
                        var log4Class = psiFacade.findClass("org.slf4j.LoggerFactory", implements.resolveScope)
                        if (log4Class != null) {
                            implements.add(factory.createImportStatement(log4Class))
                        }
                    }
                }
            }


            //日志处理类修正
            var loggerName = "logger";
            var haveLogger = false;
            psiClass.fields.filter { PsiUtil.resolveClassInType(it.type)?.qualifiedName.equals("org.slf4j.Logger") }
                    .forEach { loggerName = it.name!!;haveLogger=true; }
            if(!haveLogger) {
                /*psiClass.allFields.filter { it.name.equals(loggerName) }.forEach {
                    loggerName="log";
                }*/
                psiClass.add(factory.createFieldFromText("/**\n* 日志处理类\n*/\n" +
                        "private static final Logger logger = LoggerFactory.getLogger("+psiClass.name+".class);",psiClass))
                ;
            }
            //增加日志输出
            var logStat = factory.createStatementFromText(loggerName+"."+level+"(\"errorStack:\","
                    + (psiCatchSection.parameter as PsiParameter).name+");\n",psiCatchSection.catchBlock);
            psiCatchSection.catchBlock!!.addAfter(logStat,psiCatchSection.catchBlock!!.firstChild)
        }

    }
}
