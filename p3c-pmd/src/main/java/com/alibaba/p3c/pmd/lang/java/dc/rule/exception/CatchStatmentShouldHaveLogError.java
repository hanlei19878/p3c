package com.alibaba.p3c.pmd.lang.java.dc.rule.exception;

import com.alibaba.p3c.pmd.lang.AbstractXpathRule;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.*;
import org.jaxen.JaxenException;

import java.util.List;
import java.util.regex.Pattern;

public class CatchStatmentShouldHaveLogError extends AbstractXpathRule {
    private static final String XPATH = "//CatchStatement";
    public CatchStatmentShouldHaveLogError(){
        super.setXPath(XPATH);
    }


    @Override
    public void addViolation(Object data, Node node, String arg) {
        try {
            List<? extends Node> nodeList = node.findChildNodesWithXPath("//PrimaryExpression/PrimaryPrefix/Name[matches(@Image,'(.error){1}$')]");
            boolean isHave = false;
            if(nodeList != null &&  !nodeList.isEmpty()){
                for (Node nodeLog : nodeList) {
                    if(nodeLog.getBeginLine()>node.getEndLine()
                            ||nodeLog.getEndLine()<node.getBeginLine()){
                        continue;
                    }else{
                        ASTName astName = (ASTName) nodeLog;
                        if("Logger".
                                equals(astName.getNameDeclaration().getNode().getFirstParentOfType(ASTFieldDeclaration.class).getFirstChildOfType(ASTType.class).getFirstDescendantOfType(ASTClassOrInterfaceType.class).getImage())){
                            isHave=true;
                        }
                    }
                }
            }
            if(!isHave){
                super.addViolation(data,node,arg);
            }
        } catch (JaxenException e) {
        }
//
//        Node arg1 = argumentList.jjtGetChild(0);
//        ASTPrimaryPrefix arg1PrimaryPrefix = arg1.getFirstDescendantOfType(ASTPrimaryPrefix.class);
//        if(arg1PrimaryPrefix == null || arg1PrimaryPrefix.jjtGetNumChildren() ==0){
//            return;
//        }
//        Node arg1Val = arg1PrimaryPrefix.jjtGetChild(0);
//        String value = null;
//        if(arg1Val instanceof ASTLiteral){
//            value = arg1Val.getImage();
//        }else if(arg1Val instanceof ASTName){
//            ASTName nameVal =  (ASTName) arg1Val;
//            try {
//                value = nameVal.getNameDeclaration().getNode().jjtGetParent()
//                        .getFirstChildOfType(ASTVariableInitializer.class)
//                        .getFirstDescendantOfType(ASTLiteral.class).getImage();
//            }catch (Exception e){
//            }
//        }
//
//        if(value != null && !pattern.matcher(value).find()){
//            super.addViolation(data,node,arg);
//        }
    }
}
