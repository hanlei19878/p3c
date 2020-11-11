package com.alibaba.p3c.idea.quickfix.dc

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiTypeElementImpl
import com.intellij.psi.javadoc.PsiDocComment
import com.siyeh.InspectionGadgetsBundle
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls
import java.text.SimpleDateFormat
import java.util.*

class DcAddOrGenCommentTemplateFix : InspectionGadgetsFix() {

    private val messageKey = "com.dc.p3c.idea.quickfix.generate.javaDoc.update"
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
        //原始注释
        var lastComment = getCommentNarr(psiMethod);
        if(lastComment == null ||lastComment.equals("")){
            lastComment = "\n* TODO 功能描述 ";
        }
        //处理查询参数
        var paramBuffer = StringBuffer()
        var paraLists = psiMethod.parameterList?.parameters;
        if(paraLists!=null&&paraLists.size >0 ){
            for (paraList in paraLists) {
                var paramStr = "\n* @param "+paraList.name?:paraList.node.text;

                if(!lastComment.contains(paramStr)){
                    paramBuffer.append(paramStr);
                }else{
                    lastComment = optMethod(lastComment,paramBuffer,paramStr)
                }
            }
        }
        //处理功能详述
        var isDetail = (psiMethod.body?.text?.lines()?.size?:0)>32;
        var details = "";
        if(isDetail && !psiMethod.isConstructor){
            details="\n*\n* <p/>功能详述\n* <p/>1.TODO";
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
                var paramBufferRet = StringBuffer()
                var paramStr = "\n* @return";

                if(!lastComment.contains(paramStr)){
                    paramBuffer.append(paramStr);
                }else{
                    lastComment = optMethod(lastComment,paramBuffer,paramStr)
                }
                ret = paramBufferRet.toString();
            }
        }
//        psiMethod.children.filter {it is PsiComment &&  !(it is  PsiDocComment) }.forEach { println(it.text)}
        //处理异常
        var throwBuffer = StringBuffer()
        var throwsLists = psiMethod.throwsList?.referencedTypes;
        if(throwsLists!=null&&throwsLists.size >0 ){
            for (throwsList in throwsLists) {
                if(!lastComment.contains("@exception "+throwsList.className)) {
                    throwBuffer.append("\n* @exception ").append(throwsList.className)
                }
            }
        }

        val doc = factory.createDocCommentFromText("""
/**$lastComment $details
* ${paramBuffer.toString()}$ret${throwBuffer.toString()}${if(psiMethod.isDeprecated)"\n* @deprecated" else ""}
*
* AutoFix JavaDoc At $timeC
*/
""")
        psiMethod.addBefore(doc, psiMethod.firstChild)
        psiMethod.children.filter {it is PsiComment &&  !(it is  PsiDocComment) }.forEach { psiMethod.deleteChildRange(it,it)}
    }

    fun optMethod(lastComment:String,paramBuffer:StringBuffer,paramStr:String):String{
        var ret = lastComment
        var paramTmp = ret.substring(ret.indexOf(paramStr)).split("\n* @")[1]
        var tmpArray = paramTmp.split("\n");
        var paramComm = ""
        var i = 0;
        for (s in tmpArray) {
            if(i!= 0){
                /*if(s.indexOf("@")>=0){
                    break
                }else{*/
                paramBuffer.append(s.replace("*","").trim())
                /*}*/
            }else {
                paramBuffer.append("\n* @"+s.trim()).append(" ");
            }
            paramComm += s + "\n";
            i++;
        }
        ret =lastComment.replace("\n* @"+paramTmp,"")
        return  ret;
    }
    fun getCommentNarr(psiElement: PsiElement):String{
        //获取非doc格式注释
        var lastComment = "";
        psiElement.children.filter {it is PsiComment &&  !(it is  PsiDocComment) }.forEach {
            var temp=it.text;
            if(temp != null){
                temp = temp.trim()
                if(temp.startsWith("//")){
                    temp = temp.substring(2);
                }else if(temp.startsWith("/*")){
                    temp = temp.substring(2,temp.length-2);
                }
                temp.split("\n").forEach{
                    if(it != null ){
                        var commTemp = it.trim().replace("*","")
                                .replace("\t"," ")
                                .replace("  "," ").trim();
                        if(!commTemp.equals("")&&!commTemp.contains("AutoFix JavaDoc")){
                            lastComment += "\n* "+commTemp;
                        }
                    }
                }

            }
        }
        return lastComment;
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
        //原始注释
        var lastComment = getCommentNarr(psiClass).replace("*","* <p/>");
        if(lastComment == null ||lastComment.equals("")){
            lastComment = "\n* TODO 功能描述 ";
        }
        val doc = factory.createDocCommentFromText("""
/**
* <p/>Name: ${psiClass.name}
* <p/>
* <p/>功能描述 $lastComment ${details}
* ${if(psiClass.isDeprecated)"\n* @deprecated"  else ""}
* @author
* AutoFix JavaDoc At $timeC
* Copyright (c) DigitalChina All Rights Reserved | http://www.dcits.com
*
* 变更日期        变更人       变更简述
*———————————————————————————————————
*
*/
""")
        psiClass.addBefore(doc, psiClass.firstChild)
        psiClass.children.filter {it is PsiComment &&  !(it is  PsiDocComment) }.forEach { psiClass.deleteChildRange(it,it)}
    }

    fun doFixField(project: Project, psiField: PsiField) {
        val document = psiField.docComment
        if(document !=null){
            return;
        }
        val psiFacade = JavaPsiFacade.getInstance(project)
        val factory = psiFacade.elementFactory
        //原始注释
        var lastComment = getCommentNarr(psiField);
        if(lastComment == null ||lastComment.equals("")){
            lastComment = "\n* TODO ";
        }
        val doc = factory.createDocCommentFromText("""
/**$lastComment
*/
""")
        psiField.addBefore(doc, psiField.firstChild)
        psiField.children.filter {it is PsiComment &&  !(it is  PsiDocComment) }.forEach { psiField.deleteChildRange(it,it)}
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
        }else if(descriptor is PsiField){
            doFixField(project,descriptor as PsiField);
        }else if (descriptor.psiElement is PsiField){
            doFixField(project,descriptor.psiElement as PsiField);
        }else if (descriptor.psiElement?.parent is PsiField){
            doFixField(project,descriptor.psiElement?.parent as PsiField);
        }
    }
}