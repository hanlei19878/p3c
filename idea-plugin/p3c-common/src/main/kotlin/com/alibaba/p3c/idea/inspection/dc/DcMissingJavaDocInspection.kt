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
import com.alibaba.p3c.idea.quickfix.dc.DcAddCommentTemplateFix
import com.alibaba.p3c.idea.quickfix.dc.DcAddOrGenCommentTemplateFix
import com.alibaba.p3c.idea.util.HighlightDisplayLevels
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import javax.swing.JComponent

/**
 * Batch QuickFix Supported
 * @author caikang
 * @date 2016/12/08
 */
class DcMissingJavaDocInspection : BaseInspection, AliBaseInspection {
    private val messageKey = "com.alibaba.p3c.idea.inspection.dc.MissingJavaDocInspection"

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

    override fun ruleName(): String = "MissingJavaDocInspectionRule"

    override fun buildErrorString(vararg infos: Any): String = P3cBundle.message("$messageKey.errMsg",infos)

    override fun createOptionsPanel(): JComponent? = null

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        return DecorateInspectionGadgetsFix(DcAddCommentTemplateFix(),
                P3cBundle.getMessage("$messageKey.fix"))
    }

   /* override fun buildFixes(vararg infos: Any?): Array<InspectionGadgetsFix> {
        var fixs = arrayOf<InspectionGadgetsFix>();
        fixs = fixs.plus(DecorateInspectionGadgetsFix(DcAddCommentTemplateFix(),
                P3cBundle.getMessage("$messageKey.fix")));
        fixs = fixs.plus(DecorateInspectionGadgetsFix(DcAddOrGenCommentTemplateFix(),
                P3cBundle.getMessage("$messageKey.update.fix")));
        return fixs;
    }*/

    override fun manualBuildFix(psiElement: PsiElement, isOnTheFly: Boolean): LocalQuickFix? = buildFix(psiElement)

    override fun getDefaultLevel(): HighlightDisplayLevel = HighlightDisplayLevels.CRITICAL

    override fun buildVisitor(): BaseInspectionVisitor = MissingJavaDocVisitor()

    private inner class MissingJavaDocVisitor : BaseInspectionVisitor() {

        override fun visitClass(aClass: PsiClass) {
            val document = aClass.docComment
            if(aClass is  PsiAnonymousClass || aClass is PsiTypeParameter){
                return
            }
            if (document == null ) {
                registerClassError(aClass,aClass.name)
            }
        }
        override fun visitMethod(method: PsiMethod) {
            val document = method.docComment
            if (document == null) {
                if((method.body?.text?.lines()?.size?:9)>5) {
                    registerMethodError(method,method.name)
                }
            }
        }
    }
}
