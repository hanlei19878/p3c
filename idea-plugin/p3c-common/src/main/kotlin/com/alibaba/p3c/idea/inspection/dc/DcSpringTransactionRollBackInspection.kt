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
import com.alibaba.p3c.idea.quickfix.dc.DcAddRollBackForFix
import com.alibaba.p3c.idea.util.HighlightDisplayLevels
import com.intellij.codeHighlighting.HighlightDisplayLevel
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import javax.swing.JComponent

/**
 * Batch QuickFix Supported
 * @author caikang
 * @date 2016/12/08
 */
class DcSpringTransactionRollBackInspection : BaseInspection, AliBaseInspection {
    private val messageKey = "com.alibaba.p3c.idea.inspection.dc.SpringTransactionRollBackInspection"

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

    override fun ruleName(): String = "SpringTransactionRollBackInspectionRule"

    override fun buildErrorString(vararg infos: Any): String = P3cBundle.message("$messageKey.errMsg",infos)

    override fun createOptionsPanel(): JComponent? = null

    override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
        return DecorateInspectionGadgetsFix(DcAddRollBackForFix(),
                P3cBundle.getMessage("$messageKey.fix"))
    }

    override fun manualBuildFix(psiElement: PsiElement, isOnTheFly: Boolean): LocalQuickFix? = buildFix(psiElement)

    override fun getDefaultLevel(): HighlightDisplayLevel = HighlightDisplayLevels.BLOCKER

    override fun buildVisitor(): BaseInspectionVisitor = SpringTransactionRollBackVisitor()

    private inner class SpringTransactionRollBackVisitor : BaseInspectionVisitor() {
        override fun visitAnnotation(annotation: PsiAnnotation) {
            var anaName = annotation.getNameReferenceElement()?.referenceName;
            if(anaName != null && (anaName.equals(TRANSACTIONAL_ANNOTATION_NAME))
                    && annotation.parameterList != null && annotation.parameterList.attributes != null) {
                var newTransaction  =  false;
                var errorAttr = false;
                var exceptionAttr = false;
                var throwAttr = false;
                for (psiNameValuePair in annotation.parameterList.attributes.asList()) {
                    if(psiNameValuePair.name.equals("propagation")
                            && psiNameValuePair.text.contains("Propagation.REQUIRES_NEW")){
                        newTransaction = true;
                    }else if(psiNameValuePair.name.equals(ROLL_BACK_FOR)){
                        var rollBackAttrs =  psiNameValuePair.value as? PsiArrayInitializerMemberValue;
                        if(rollBackAttrs != null && rollBackAttrs.initializers != null){
                            for (initializer in rollBackAttrs.initializers) {
                                var psiType = (initializer as? PsiClassObjectAccessExpression)?.operand?.type?:continue
                                var initName="";
                                if(psiType is PsiJavaCodeReferenceElement){
                                    initName = (psiType as PsiJavaCodeReferenceElement)?.getCanonicalText()
                                }else if(psiType is PsiClassReferenceType){
                                    initName = (psiType as PsiClassReferenceType)?.reference?.getCanonicalText()
                                }
                                if(initName == null)
                                    continue;
                                if(ERROR_CLASS.equals(initName)){
                                    errorAttr = true;
                                }else if(EXCEPTION_CLASS.equals(initName)){
                                    exceptionAttr = true;
                                }else if(THROWABLE_CLASS.equals(initName)){
                                    throwAttr = true;
                                }
                            }
                        }
                    }
//                    else if(psiNameValuePair.name.equals(ROLL_BACK_FOR_CLASS_NAME)){
//                        var errorAttr = false;
//                        var exceptionAttr = false;
//                        var rollBackAttrs =  psiNameValuePair.value as PsiArrayInitializerMemberValue;
//                        if(rollBackAttrs != null && rollBackAttrs.initializers != null){
//                            for (initializer in rollBackAttrs.initializers) {
//                                var initName = ((initializer as PsiClassObjectAccessExpression)?.
//                                        operand.type as PsiJavaCodeReferenceElement)?.
//                                        getCanonicalText();
//                                if(initName == null)
//                                    continue;
//                                if(ERROR_CLASS.equals(initName)){
//                                    errorAttr = true;
//                                }else if(EXCEPTION_CLASS.equals(initName)){
//                                    exceptionAttr = true;
//                                }
//                            }
//                        }
//                    }
                }

                if(newTransaction && !(errorAttr && exceptionAttr) && !throwAttr){
                    registerError(annotation);
                }
            };
        }

    }

    private val TRANSACTIONAL_ANNOTATION_NAME = "Transactional"
    private val TRANSACTIONAL_FULL_NAME = "org.springframework.transaction.annotation.Transactional"
    private val ROLL_BACK_FOR = "rollbackFor"
    private val ROLL_BACK_FOR_CLASS_NAME = "noRollbackForClassName"
    private val ERROR_CLASS = "java.lang.Error"
    private val EXCEPTION_CLASS = "java.lang.Exception"
    private val THROWABLE_CLASS = "java.lang.Throwable"

}
