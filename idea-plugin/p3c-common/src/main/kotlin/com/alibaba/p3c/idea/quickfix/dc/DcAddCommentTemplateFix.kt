package com.alibaba.p3c.idea.quickfix.dc

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiTypeElementImpl
import com.intellij.psi.javadoc.PsiDocComment
import com.siyeh.InspectionGadgetsBundle
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls
import java.text.SimpleDateFormat
import java.util.*

class DcAddCommentTemplateFix : InspectionGadgetsFix() {

    private val messageKey = "com.dc.p3c.idea.quickfix.generate.javaDoc"
    val user = "${System.getenv("USER") ?: System.getProperty("user.name")}"
    var dateC = SimpleDateFormat("yyyy/MM/dd").format(Date())
    var timeC =  SimpleDateFormat("yyyy/MM/dd HH:mm").format(Date())

    @Nls
    override fun getName(): String {
        return InspectionGadgetsBundle.message(messageKey, *arrayOfNulls(0))
    }

    override fun getFamilyName(): String {
        return this.name
    }


    fun doFixMethod(project: Project, psiMethod: PsiMethod) {
        val document = psiMethod.docComment
        if(document !=null){
            return;
        }
        val psiFacade = JavaPsiFacade.getInstance(project)
        val factory = psiFacade.elementFactory
        //处理查询参数
        var paramBuffer = StringBuffer()
        var paraLists = psiMethod.parameterList?.parameters;
        if(paraLists!=null&&paraLists.size >0 ){
            for (paraList in paraLists) {
                paramBuffer.append("\n* @param ").append(paraList.name?:paraList.node.text);
            }
        }
        //处理功能详述
        var isDetail = (psiMethod.body?.text?.lines()?.size?:0)>32;
        var details = "";
        if(isDetail && !psiMethod.isConstructor){
            details="\n*\n* <p/>功能详述\n* <p/>1.TODO\n* <p/>2.TODO\n* <p/>3.TODO\n";
        }
        var ret = "";
        //处理返回
        if(psiMethod.returnType != null){
            var isVoid = false;
            try {
                if((psiMethod.returnTypeElement as PsiTypeElementImpl).text.equals("void")){
                    isVoid = true;
                }
            } catch (e: Exception) {
            }
            if(!isVoid) {
                ret = "\n* @return ";
            }
        }
        psiMethod.children.filter {it is PsiComment &&  !(it is  PsiDocComment) }.forEach { println(it.text)}
        //处理异常
        var throwBuffer = StringBuffer()
        var throwsLists = psiMethod.throwsList?.referencedTypes;
        if(throwsLists!=null&&throwsLists.size >0 ){
            for (throwsList in throwsLists) {
                throwBuffer.append("\n* @exception ").append(throwsList.className)
            }
        }

        val doc = factory.createDocCommentFromText("""
/**
* TODO 功能描述 $details
* ${paramBuffer.toString()}$ret${throwBuffer.toString()}${if(psiMethod.isDeprecated)"\n* @deprecated" else ""}
*
* Create At $timeC By $user
*/
""")
        psiMethod.addBefore(doc, psiMethod.firstChild)
    }

    fun doFixClass(project: Project,psiClass: PsiClass) {
        val document = psiClass.docComment
        if(document !=null){
            return;
        }
        val psiFacade = JavaPsiFacade.getInstance(project)
        val factory = psiFacade.elementFactory
        var details ="";
        if(psiClass.isAnnotationType||psiClass.isEnum||psiClass.isInterface){

        }else {
            details = "\n* <p/>\n" +
                    "* <p/>功能范围\n" +
                    "* <p/>1.TODO\n" +
                    "* <p/>2.TODO\n" +
                    "* <p/>3.TODO"
        }
        val doc = factory.createDocCommentFromText("""
/**
* <p/>Name: ${psiClass.name}
* <p/>
* <p/>功能描述
* <p/>TODO 功能描述${details}
* ${if(psiClass.isDeprecated)"\n* @deprecated"  else ""}
* @author $user
* Create $timeC
* Copyright (c) DigitalChina All Rights Reserved | http://www.dcits.com
*
* 变更日期        变更人       变更简述
*———————————————————————————————————
*
*/
""")
        psiClass.addBefore(doc, psiClass.firstChild)
    }
    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        descriptor ?: return
        if(descriptor is PsiMethod){
            doFixMethod(project,descriptor as PsiMethod);
        }else if (descriptor.psiElement is PsiMethod){
            doFixMethod(project,descriptor.psiElement as PsiMethod);
        }else if (descriptor.psiElement?.parent is PsiMethod){
            doFixMethod(project,descriptor.psiElement?.parent as PsiMethod);
        } else if(descriptor is PsiClass){
            doFixClass(project,descriptor as PsiClass);
        }else if (descriptor.psiElement is PsiClass){
            doFixClass(project,descriptor.psiElement as PsiClass);
        }else if (descriptor.psiElement?.parent is PsiClass){
            doFixClass(project,descriptor.psiElement?.parent as PsiClass);
        }
    }
}