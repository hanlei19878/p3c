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
package com.alibaba.p3c.pmd.lang.java.dc.rule.flowcontrol;

import com.alibaba.p3c.pmd.lang.AbstractXpathRule;
import com.alibaba.p3c.pmd.lang.java.util.ViolationUtils;
import com.alibaba.p3c.pmd.lang.java.util.namelist.NameListConfig;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.*;
import net.sourceforge.pmd.lang.symboltable.NameDeclaration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * [Mandatory] Do not use complicated statements in conditional statements (except for frequently used methods
 * like getXxx/isXxx). Use boolean variables to store results of complicated statements temporarily will increase
 * the code's readability.
 *
 * @author zenghou.fw
 * @date 2017/04/11
 */
public class AvoidProcessInLogIfRule extends AbstractXpathRule {
    private static final String XPATH = "((//IfStatement/Expression/ConditionalAndExpression|//IfStatement/Expression)" +
            "/PrimaryExpression/PrimaryPrefix"+
            "[./Name[(matches(@Image, '[.]{1}" +
            "((isDebugEnabled)|(isTraceEnabled)|(isInfoEnabled)|(isWarnEnabled)|(isErrorEnabled)){1}" +
            "{1}'))" +
            "]])";
    private static final Set<String> WHITE_LIST = new HashSet<>(NameListConfig.NAME_LIST_SERVICE.getNameList(
            "AvoidProcessInLogIfRule", "WHITE_LIST"));

    public AvoidProcessInLogIfRule() {
        setXPath(XPATH);
    }



    @Override
    public void addViolation(Object data, Node node, String arg) {
        if(node instanceof ASTPrimaryPrefix){
            ASTName logVarNameNode = node.getFirstDescendantOfType(ASTName.class);
            if(logVarNameNode == null){
                return;
            }
            NameDeclaration methodCallVir = logVarNameNode.getNameDeclaration();
            if(methodCallVir == null || methodCallVir == null){
                return;
            }
            String logVirName =  methodCallVir.getImage();
            ASTStatement baseStatement =  node.getFirstParentOfType(ASTIfStatement.class).getFirstChildOfType(ASTStatement.class);
            if(baseStatement == null){
                return;
            }
            List<ASTStatementExpression> statementExpressionList = baseStatement.findDescendantsOfType(ASTStatementExpression.class);
            if(statementExpressionList == null){
                return;
            }
            for (Node statement : statementExpressionList) {
                ASTStatementExpression statementExpression = (ASTStatementExpression) statement;
                ASTName callMethod = statementExpression.getFirstDescendantOfType(ASTPrimaryPrefix.class).getFirstChildOfType(ASTName.class);
                if(callMethod.getNameDeclaration() == null && WHITE_LIST.contains(callMethod.getImage())){
                    //白名单不处理
                    continue;
                }else if(callMethod.getNameDeclaration() != null && callMethod.getNameDeclaration().getImage().equals(logVirName)){
                    //日志类方法不处理
                    continue;
                }

                ViolationUtils.addViolationWithPrecisePosition(this, statementExpression, data,
                        "java.flowcontrol.dc.AvoidProcessInLogIfRule.violation.msg");
            }
        }
    }
}
