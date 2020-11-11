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
import net.sourceforge.pmd.lang.java.ast.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * [Mandatory] Make sure to invoke the rollback if a method throws an Exception.
 *
 * @author caikang
 * @date 2017/03/29
 */
public class SpringTransactionRollBackForRule extends AbstractAliRule {
    private static final String TRANSACTIONAL_ANNOTATION_NAME = "Transactional";
    private static final String TRANSACTIONAL_FULL_NAME = "org.springframework.transaction.annotation."
            + TRANSACTIONAL_ANNOTATION_NAME;
    private static final String MESSAGE_KEY_PREFIX = "java.spring.dc.SpringTransactionRollBackForRule.violation.msg";

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
        ASTMemberValuePairs astMemberValuePairs = node.getFirstDescendantOfType(ASTMemberValuePairs.class);
        if(astMemberValuePairs == null || astMemberValuePairs.jjtGetNumChildren() ==0){
            return super.visit(node, data);
        }
        List<ASTMemberValuePair> memberValuePairs = astMemberValuePairs.findChildrenOfType(ASTMemberValuePair.class);
        if(memberValuePairs == null || memberValuePairs.isEmpty()){
            return super.visit(node, data);
        }
        boolean newTransaction = false;
        boolean errorRollback = false;
        boolean excpetionRollback = false;
        for (ASTMemberValuePair memberValuePair : memberValuePairs) {
            if(memberValuePair.getImage() !=null
                    &&"propagation".equals(memberValuePair.getImage())){
                ASTName astName = memberValuePair.getFirstDescendantOfType(ASTName.class);
                if(astName != null) {
                    if ("Propagation.REQUIRES_NEW".equals(astName.getImage())) {
                        newTransaction = true;
                    }
                }
            }else if(memberValuePair.getImage() !=null
                    &&"rollbackFor".equals(memberValuePair.getImage())){
                List<ASTType> nodes = memberValuePair.findDescendantsOfType(ASTType.class);
                if(nodes != null && !nodes.isEmpty()) {
                    for (ASTType astType : nodes) {
                        if (astType.getType() != null) {
                            if (astType.getType().equals(Error.class)) {
                                errorRollback = true;
                            } else if (astType.getType().equals(Exception.class)) {
                                excpetionRollback = true;
                            }
                        }
                    }
                }
            }
        }

        if(newTransaction && (!errorRollback ||!excpetionRollback)){
            addViolationWithMessage(data, node, MESSAGE_KEY_PREFIX);
        }
        return super.visit(node, data);
    }
}
