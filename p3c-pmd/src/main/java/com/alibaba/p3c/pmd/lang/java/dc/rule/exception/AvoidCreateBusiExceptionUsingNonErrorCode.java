package com.alibaba.p3c.pmd.lang.java.dc.rule.exception;

import com.alibaba.p3c.pmd.lang.AbstractXpathRule;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.*;

import java.util.regex.Pattern;

public class AvoidCreateBusiExceptionUsingNonErrorCode extends AbstractXpathRule {
    // 创建 Pattern 对象
    private  final Pattern pattern= Pattern.compile("^[\"]*[A-Z]{2,4}[0-9]{4,6}[\"]*$");
    private static final String XPATH = "//ThrowStatement/Expression/PrimaryExpression" +
            "[./PrimaryPrefix/Name[matches(@Image,'" +
            "((BusiUtil)|(BusinessUtils)){1}[ .]*" +
            "((createBusinessException)|(createWithoutAuthorizationException)|(createWithoutConfirmationException)|(throwException)){1}" +
            "')" +
            "]]";
    public AvoidCreateBusiExceptionUsingNonErrorCode(){
        super.setXPath(XPATH);
    }


    @Override
    public void addViolation(Object data, Node node, String arg) {
        ASTArgumentList argumentList = node.getFirstChildOfType(ASTPrimarySuffix.class).getFirstDescendantOfType(ASTArgumentList.class);
        if(argumentList == null || argumentList.jjtGetNumChildren()==0){
            return;
        }
        Node arg1 = argumentList.jjtGetChild(0);
        ASTPrimaryPrefix arg1PrimaryPrefix = arg1.getFirstDescendantOfType(ASTPrimaryPrefix.class);
        if(arg1PrimaryPrefix == null || arg1PrimaryPrefix.jjtGetNumChildren() ==0){
            return;
        }
        Node arg1Val = arg1PrimaryPrefix.jjtGetChild(0);
        String value = null;
        if(arg1Val instanceof ASTLiteral){
            value = arg1Val.getImage();
        }else if(arg1Val instanceof ASTName){
            ASTName nameVal =  (ASTName) arg1Val;
            try {
                value = nameVal.getNameDeclaration().getNode().jjtGetParent()
                        .getFirstChildOfType(ASTVariableInitializer.class)
                        .getFirstDescendantOfType(ASTLiteral.class).getImage();
            }catch (Exception e){
            }
        }

        if(value != null && !pattern.matcher(value).find()){
            super.addViolation(data,node,arg);
        }
    }
}
