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
package com.alibaba.p3c.pmd.lang.java.dc.rule.spring;

import com.alibaba.p3c.pmd.lang.java.rule.AbstractAliRule;
import com.alibaba.p3c.pmd.lang.java.util.namelist.NameListConfig;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.*;
import org.jaxen.JaxenException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * [Mandatory] Make sure to invoke the rollback if a method throws an Exception.
 *
 * @author caikang
 * @date 2017/03/29
 */
public class SpringTransactionMethodDeclareRule extends AbstractAliRule {
    private static final String TRANSACTIONAL_ANNOTATION_NAME = "Transactional";
    private static final String TRANSACTIONAL_FULL_NAME = "org.springframework.transaction.annotation."
        + TRANSACTIONAL_ANNOTATION_NAME;
    private static final String MESSAGE_KEY_PREFIX = "java.spring.dc.SpringTransactionMethodDeclareRule.violation.msg";

    private static final Set<String> SPRING_SERVICE_ANNOTATION_NAMES = new HashSet<>(NameListConfig.NAME_LIST_SERVICE.getNameList(
            "SpringServiceBean", "ANNOTATION_NAME_LIST"));
    @Override
    public Object visit(ASTAnnotation node, Object data) {
        ASTName name = node.getFirstDescendantOfType(ASTName.class);
        boolean noTransactional = name == null || !(TRANSACTIONAL_ANNOTATION_NAME.equals(name.getImage())
            && !TRANSACTIONAL_FULL_NAME.equals(name.getImage()));
        //非事务注解或类注解不处理
        if (noTransactional|| node.jjtGetParent() instanceof ASTTypeDeclaration ) {
            return super.visit(node, data);
        }
        //查询方法定义
        ASTMethodDeclaration methodDeclaration = node.jjtGetParent().getFirstDescendantOfType(ASTMethodDeclaration.class);
        if(methodDeclaration == null){
            addViolationWithMessage(data, node, MESSAGE_KEY_PREFIX);
            return super.visit(node, data);
        }
        if(!methodDeclaration.isPublic()){
            addViolationWithMessage(data, node, MESSAGE_KEY_PREFIX,new Object[] {methodDeclaration.getMethodName()});
            return super.visit(node, data);
        }
        if(methodDeclaration.isStatic()||methodDeclaration.isAbstract()){
            addViolationWithMessage(data, node, MESSAGE_KEY_PREFIX,new Object[] {methodDeclaration.getMethodName()});
            return super.visit(node, data);
        }
        ASTCompilationUnit compilationUnit = node.getFirstParentOfType(ASTCompilationUnit.class);
        try {
            List<Node> nodes= compilationUnit.findChildNodesWithXPath("//PrimaryExpression[./PrimaryPrefix/Name[matches(@Image,'^("+methodDeclaration.getName()+"){1}$')]]");
            if(nodes != null && !nodes.isEmpty()){
                for (Node nodeCla : nodes) {
                    if(nodeCla.getFirstDescendantOfType(ASTArguments.class).getArgumentCount()
                        == methodDeclaration.getFirstDescendantOfType(ASTFormalParameters.class).getParameterCount()) {
                        addViolationWithMessage(data, nodeCla, MESSAGE_KEY_PREFIX, new Object[]{methodDeclaration.getMethodName()});
                    }
                }
            }

            nodes= compilationUnit.findChildNodesWithXPath("//PrimaryExpression[./PrimarySuffix[matches(@Image,'^("+methodDeclaration.getName()+"){1}$')]]");
            if(nodes != null && !nodes.isEmpty()){
                for (Node nodeCla : nodes) {
                    if(nodeCla.getFirstDescendantOfType(ASTPrimaryPrefix.class).usesThisModifier()
                    && nodeCla.getFirstDescendantOfType(ASTArguments.class).getArgumentCount()
                            == methodDeclaration.getFirstDescendantOfType(ASTFormalParameters.class).getParameterCount()) {
                        addViolationWithMessage(data, nodeCla, MESSAGE_KEY_PREFIX, new Object[]{methodDeclaration.getMethodName()});
                    }
                }
            }
        } catch (JaxenException e) {
        }

        //查询类定义
        ASTTypeDeclaration typeDeclaration =  node.getFirstParentOfType(ASTTypeDeclaration.class);
        List<ASTAnnotation> annotations =typeDeclaration.findChildrenOfType(ASTAnnotation.class);
        //类注解标签
        if(annotations == null){
            addViolationWithMessage(data, node, MESSAGE_KEY_PREFIX,
                    new Object[] {methodDeclaration.getMethodName()});
        }else{
            boolean notService = true;
            for (ASTAnnotation annotation : annotations) {
                ASTName anName = annotation.getFirstDescendantOfType(ASTName.class);
                if(SPRING_SERVICE_ANNOTATION_NAMES.contains(anName.getImage())){
                    notService = false;
                    break;
                }
            }
            if(notService){
                addViolationWithMessage(data, node, MESSAGE_KEY_PREFIX,
                        new Object[] {methodDeclaration.getMethodName()});
            }
        }
        return super.visit(node, data);
    }
}
