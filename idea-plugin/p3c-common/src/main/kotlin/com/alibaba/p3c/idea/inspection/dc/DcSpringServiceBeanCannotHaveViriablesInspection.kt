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
class DcSpringServiceBeanCannotHaveViriablesInspection : BaseInspection, AliBaseInspection {
    private val messageKey = "com.alibaba.p3c.idea.inspection.dc.SpringServiceBeanCannotHaveViriablesInspection"

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

    override fun ruleName(): String = "SpringServiceBeanCannotHaveViriablesRule"

    override fun buildErrorString(vararg infos: Any): String = P3cBundle.message("$messageKey.errMsg",infos)

    override fun createOptionsPanel(): JComponent? = null

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        return DecorateInspectionGadgetsFix(DcAddFinalKeyWordFix(),
                P3cBundle.getMessage("$messageKey.fix"))
    }

    override fun manualBuildFix(psiElement: PsiElement, isOnTheFly: Boolean): LocalQuickFix? = buildFix(psiElement)

    override fun getDefaultLevel(): HighlightDisplayLevel = HighlightDisplayLevels.BLOCKER

    override fun buildVisitor(): BaseInspectionVisitor = SpringServiceBeanCannotHaveViriables()

    private inner class SpringServiceBeanCannotHaveViriables : BaseInspectionVisitor() {

        override fun visitClass(aClass: PsiClass) {
            if(aClass is  PsiAnonymousClass || aClass is PsiTypeParameter){
                return
            }
            var annotations = aClass.modifierList?.annotations?:return
            var isService = false;
            for (annotation in annotations) {
                var anaName = annotation.nameReferenceElement?.referenceName?:continue
                if(SPRING_SERVICE_ANNOTATION_NAMES.contains(anaName)){
                    isService = true;
                    break;
                }
            }
            if(!isService){
                return
            }

            var springAppClass = false
            var implementsLists = aClass.implementsList
            if(implementsLists != null){
                implementsLists.referenceElements?.forEach {
                    if(it.canonicalText.equals("com.dcits.ensemble.product.api.application.IProductPart")){
                        return
                    }else if(it.canonicalText.equals("org.springframework.context.ApplicationContextAware")
                            ||it.canonicalText.equals("org.springframework.context.ApplicationListener")){
                        springAppClass = true;
                    }
                }
                if(!springAppClass) {
                    implementsLists.referencedTypes?.forEach {
                        if((it.resolve()?.qualifiedName?.equals("org.springframework.context.ApplicationContextAware")?:false
                                        ||it.resolve()?.qualifiedName?.equals("org.springframework.context.ApplicationListener")?:false)){
                            springAppClass = true;
                        }
                    }
                }
            }

            for (allField in aClass.fields) {
                if(allField.containingFile ==null || !allField.containingFile.name.endsWith(".java")){
                    continue;
                }
//
//                if(!allField.isPhysical){
//                    continue
//                }
                //Logger属性变量不处理
                try {
                    val typeName = allField.typeElement?.text
                    if(typeName !=null) {
                        if ("Logger".equals(typeName) ||  typeName.indexOf( "ThreadLocal")>=0) {
                            continue
                        }
                    }
                } catch (e: Exception) {
                    //                e.printStackTrace();
                }

                var findAnnotation = false;
                var keys = allField.modifierList?.children?:continue
                var isCheck = true;
                if(springAppClass && aClass.name?.endsWith("Factory")?:false) {
                    isCheck = false;
                }else {
                    for (key in keys) {
                        if (key is PsiKeyword) {
                            var keyT = key as PsiKeyword;
                            if (keyT.text.equals("final")) {
                                isCheck = false;
                                break;
                            }
                        } else if (key is PsiAnnotation) {
                            var keyT = key as PsiAnnotation;
                            var anaName = keyT.nameReferenceElement?.referenceName ?: continue
                            if (anaName.equals("Resource") || "Autowired".equals(anaName)) {
                                isCheck = false;
                                break;
                            }
                        }
                    }
                }
                if(isCheck) {
                    try {
                        registerError(allField, allField.name)
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    private val SPRING_SERVICE_ANNOTATION_NAMES = HashSet(NameListConfig.NAME_LIST_SERVICE.getNameList(
            "SpringServiceBean", "ANNOTATION_NAME_LIST"))


    class DcAddFinalKeyWordFix : InspectionGadgetsFix() {

        private val messageKey = "com.alibaba.p3c.idea.inspection.dc.SpringServiceBeanCannotHaveViriablesInspection.fix"

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
            field.modifierList?.setModifierProperty(PsiModifier.STATIC, true)
            field.modifierList?.setModifierProperty(PsiModifier.FINAL, true)
        }


        private val LOGGER = Logger.getInstance(DcAddFinalKeyWordFix::class.java)
    }
}
