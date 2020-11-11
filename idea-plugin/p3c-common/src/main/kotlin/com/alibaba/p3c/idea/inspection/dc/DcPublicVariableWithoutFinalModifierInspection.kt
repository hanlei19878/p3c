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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.siyeh.InspectionGadgetsBundle
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls
import java.util.*
import javax.swing.JComponent

/**
 * Batch QuickFix Supported
 * @author caikang
 * @date 2016/12/08
 */
class DcPublicVariableWithoutFinalModifierInspection : BaseInspection, AliBaseInspection {
    private val messageKey = "com.alibaba.p3c.idea.inspection.dc.PublicVariableWithoutFinalModifierInspection"

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

    override fun ruleName(): String = "PublicVariableWithoutFinalModifierRule"

    override fun buildErrorString(vararg infos: Any): String {
        return String.format(P3cBundle.getMessage("$messageKey.errMsg"), infos[0])
    } //P3cBundle.message("$messageKey.errMsg",infos)

    override fun createOptionsPanel(): JComponent? = null

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        return DecorateInspectionGadgetsFix(DcMakePrivateFix(),
                P3cBundle.getMessage("$messageKey.Private.fix"));
    }

    override fun buildFixes(vararg infos: Any?): Array<InspectionGadgetsFix> {
        var fixs = arrayOf<InspectionGadgetsFix>();
        fixs = fixs.plus(DecorateInspectionGadgetsFix(DcMakePrivateFix(),
                P3cBundle.getMessage("$messageKey.Private.fix")));
        fixs = fixs.plus(DecorateInspectionGadgetsFix(DcMakeStaticFinalFix(),
                P3cBundle.getMessage("$messageKey.StaticFinal.fix")));
        fixs = fixs.plus(DecorateInspectionGadgetsFix(DcMakeProtectedFix(),
                P3cBundle.getMessage("$messageKey.Protected.fix")));
        return fixs;
    }

    override fun manualBuildFix(psiElement: PsiElement, isOnTheFly: Boolean): LocalQuickFix? = buildFix(psiElement)

    override fun getDefaultLevel(): HighlightDisplayLevel = HighlightDisplayLevels.CRITICAL;

    override fun buildVisitor(): BaseInspectionVisitor = PublicVariableWithoutFinalModifier()

    private inner class PublicVariableWithoutFinalModifier : BaseInspectionVisitor() {

        override fun visitClass(aClass: PsiClass) {
            if(aClass is  PsiAnonymousClass || aClass is PsiTypeParameter){
                return
            }
            for (allField in aClass.fields) {
                if(allField.containingFile ==null || !allField.containingFile.name.endsWith(".java")){
                    continue;
                }
                var keys = allField.modifierList?.children?:continue
                var isFinal = false;
                var isPrivate = false;
                var isPublic = false;
                for (key in keys) {
                    if(key is PsiKeyword){
                        var keyT = key as PsiKeyword;
                        if(keyT.text.equals("final")){
                            isFinal = true;
                            break;
                        }else if(keyT.text.equals(PsiKeyword.PUBLIC)){
                            isPublic = true;
                        }else if(keyT.text.equals(PsiKeyword.PRIVATE)){
                            isPrivate = true;
                            break;
                        }
                    }
                }
                if(isPublic&&!isPrivate&&!isFinal) {
                    try {
                        registerError(allField, allField.name)
                    } catch (e: Exception) {

                    }
                }
            }
        }
    }

    class DcMakeStaticFinalFix : InspectionGadgetsFix() {

        private val messageKey = "com.alibaba.p3c.idea.inspection.dc.PublicVariableWithoutFinalModifierInspection.StaticFinal.fix"

        @Nls
        override fun getName(): String {
            return InspectionGadgetsBundle.message(messageKey, *arrayOfNulls(0))
        }

        override fun getFamilyName(): String {
            return this.name
        }

        override fun doFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor?.psiElement?: return
            var field = descriptor.psiElement as PsiField?: return
            val modifierList = field.modifierList ?: return

            modifierList.setModifierProperty(PsiModifier.STATIC, true)
            modifierList.setModifierProperty(PsiModifier.FINAL, true)
        }


        private val LOGGER = Logger.getInstance(DcMakeStaticFinalFix::class.java)
    }


    class DcMakePrivateFix : InspectionGadgetsFix() {

        private val messageKey = "com.alibaba.p3c.idea.inspection.dc.PublicVariableWithoutFinalModifierInspection.Private.fix"

        @Nls
        override fun getName(): String {
            return InspectionGadgetsBundle.message(messageKey, *arrayOfNulls(0))
        }

        override fun getFamilyName(): String {
            return this.name
        }

        override fun doFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor?.psiElement?: return
            var field = descriptor.psiElement as PsiField?: return
            val modifierList = field.modifierList ?: return
            modifierList.setModifierProperty(PsiModifier.PRIVATE ,true)
        }


        private val LOGGER = Logger.getInstance(DcMakePrivateFix::class.java)
    }


    class DcMakeProtectedFix : InspectionGadgetsFix() {

        private val messageKey = "com.alibaba.p3c.idea.inspection.dc.PublicVariableWithoutFinalModifierInspection.Protected.fix"

        @Nls
        override fun getName(): String {
            return InspectionGadgetsBundle.message(messageKey, *arrayOfNulls(0))
        }

        override fun getFamilyName(): String {
            return this.name
        }

        override fun doFix(project: Project, descriptor: ProblemDescriptor) {
            descriptor?.psiElement?: return
            var field = descriptor.psiElement as PsiField?: return
            val modifierList = field.modifierList ?: return
            modifierList.setModifierProperty(PsiModifier.PROTECTED ,true)
        }


        private val LOGGER = Logger.getInstance(DcMakeProtectedFix::class.java)
    }
}
