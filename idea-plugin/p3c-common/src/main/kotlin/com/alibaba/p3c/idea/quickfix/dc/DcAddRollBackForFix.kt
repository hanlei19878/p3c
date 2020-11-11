package com.alibaba.p3c.idea.quickfix.dc

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.siyeh.InspectionGadgetsBundle
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls

class DcAddRollBackForFix : InspectionGadgetsFix() {

    private val messageKey = "com.alibaba.p3c.idea.inspection.dc.SpringTransactionRollBackInspection.fix"

    @Nls
    override fun getName(): String {
        return InspectionGadgetsBundle.message(messageKey, *arrayOfNulls(0))
    }

    override fun getFamilyName(): String {
        return this.name
    }

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        descriptor?.psiElement?: return
        var anaDes = descriptor.psiElement;
        if(anaDes is PsiAnnotation){
            var annParent =  anaDes.parent
            var annotation = anaDes as PsiAnnotation;
            var anaName = annotation.getNameReferenceElement()?.referenceName;
            if(anaName != null && (anaName.equals(TRANSACTIONAL_ANNOTATION_NAME))){
                var newText = annotation.text;
                var newTransaction  =  false;
                for (psiNameValuePair in annotation.parameterList.attributes.asList()) {
                    if(psiNameValuePair.name.equals("propagation")
                            && psiNameValuePair.text.contains("Propagation.REQUIRES_NEW")){
                        newTransaction = true;
                    }else if(psiNameValuePair.name.equals(ROLL_BACK_FOR)){
                        newText.replace(psiNameValuePair.text,ROLL_BACK_FOR_ATTRS)
                    }
                }
                if(!newText.contains(ROLL_BACK_FOR_ATTRS)){
                    newText = newText.substring(0,newText.length-1)+","+ROLL_BACK_FOR_ATTRS+")"
                }
                if(newTransaction){
                    val psiFacade = JavaPsiFacade.getInstance(project)
                    val factory = psiFacade.elementFactory
                    val annotationNew = factory.createAnnotationFromText(newText, anaDes.getParent())
                    annParent.addAfter(annotationNew,annotation)
                    annParent.deleteChildRange(annotation,annotation)
                }
            }
        }
    }

    private val TRANSACTIONAL_ANNOTATION_NAME = "Transactional"
    private val ROLL_BACK_FOR = "rollbackFor"
    private val ROLL_BACK_FOR_ATTRS = "rollbackFor = {Exception.class,Error.class}"

    private val LOGGER = Logger.getInstance(DcAddRollBackForFix::class.java)
}