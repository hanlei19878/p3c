package com.alibaba.p3c.pmd.lang.java.dc.rule.other;

import com.alibaba.p3c.pmd.lang.AbstractXpathRule;
import com.alibaba.p3c.pmd.lang.java.util.ViolationUtils;
import com.alibaba.p3c.pmd.lang.java.util.namelist.NameListConfig;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.*;
import net.sourceforge.pmd.lang.java.symboltable.MethodNameDeclaration;
import net.sourceforge.pmd.lang.java.symboltable.VariableNameDeclaration;
import net.sourceforge.pmd.lang.symboltable.NameDeclaration;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Name: AvoidNewBigDecimalNonStringRule</p>
 * <p/>
 * <p>@descrption:</p>
 * <p>功能描述</p>
 * <p>{todo}</p>
 * <p>功能范围</p>
 * <p>1.{todo}</p>
 * <p>2.</p>
 * <p>3.</p>
 *
 * @author hanlei
 * Create at 2019/3/8 10:26
 * <p>
 * Copyright @ dcitc.com . All Rights Reserved.
 */
public class AvoidNewBigDecimalNonStringRule  extends AbstractXpathRule {
    private static final String XPATH = "//AllocationExpression[./ClassOrInterfaceType[@Image='BigDecimal']]";

    private static final Set<String> TYPES = new HashSet<>(NameListConfig.NAME_LIST_SERVICE.getNameList(
            "AvoidNewBigDecimalNonStringRule", "TYPE_LIST"));
    public AvoidNewBigDecimalNonStringRule(){
        super.setXPath(XPATH);
    }

    @Override
    public void addViolation(Object data, Node node, String arg) {
        ASTArgumentList argumentList = node.getFirstDescendantOfType(ASTArgumentList.class);
        //无参数或参数数量不为1时则不处理
        if(argumentList == null || argumentList.jjtGetNumChildren() != 1){
            return;
        }

        for (int i = 0; i < argumentList.jjtGetNumChildren(); i++) {
            Node argument = argumentList.jjtGetChild(i);
            ASTAdditiveExpression additiveExpression = argument.getFirstChildOfType(ASTAdditiveExpression.class);
            ASTPrimaryExpression astPrimaryExpression = argument.getFirstChildOfType(ASTPrimaryExpression.class);
            if(additiveExpression !=null){
                boolean flag = true;
                for (int i1 = 0; i1 < additiveExpression.jjtGetNumChildren(); i1++) {
                    Node advChild = additiveExpression.jjtGetChild(i1);
                    if(advChild instanceof  ASTPrimaryExpression){
                        if(isStringOrObjReturn((ASTPrimaryExpression)advChild)){
                            flag = false;
                            break;
                        }
                    }
                }
                if(flag){
                    ViolationUtils.addViolationWithPrecisePosition(this, node, data);
                }
            }else if(astPrimaryExpression != null) {
                if(!isStringOrObjReturn(astPrimaryExpression)){
                    ViolationUtils.addViolationWithPrecisePosition(this, node, data);
                }
            } else {
                System.out.println("UnKnow line:"+node.getBeginLine());
            }
        }
    }

    /**
     * 判定返回结果是否为字符串或对象（不处理）
     * @param astPrimaryExpression
     * @return
     */
    private boolean isStringOrObjReturn(ASTPrimaryExpression astPrimaryExpression){
        boolean returnFlag = false;
        for (int i = 0; i < astPrimaryExpression.jjtGetNumChildren() && !returnFlag; i++) {
            Node child =  astPrimaryExpression.jjtGetChild(i);
            //单一表达式赋值处理
            if(child instanceof ASTPrimaryPrefix && astPrimaryExpression.jjtGetNumChildren() <=2){
                try {
                    if (acceptReturnType((ASTPrimaryPrefix) child)) {
                        returnFlag = true;
                        break;
                    }
                }catch (Exception e){
                    returnFlag = true;
                    break;
                }
            }else if(child instanceof ASTPrimarySuffix){
                continue;
            }else{
                returnFlag = true;
            }
        }
        return  returnFlag;
    }


    /**
     * 返回结果类型是否接受
     * @param primaryPrefix
     * @return
     */
    private boolean acceptReturnType(ASTPrimaryPrefix primaryPrefix){
        //获取声明
        Node preChild = primaryPrefix.jjtGetChild(0);
        //使用固定值
        if(preChild instanceof ASTLiteral){
            ASTLiteral astLiteral = (ASTLiteral) preChild;
            if(astLiteral.isIntLiteral()
                    ||astLiteral.isDoubleLiteral()
                    ||astLiteral.isFloatLiteral()
                    ||astLiteral.isLongLiteral()){
                return false;
            }
        }else if(preChild instanceof  ASTName){
            //使用变量
            ASTName variableName = (ASTName)preChild;
            //查询变量声明
            NameDeclaration tmpNameDeclaration= variableName.getNameDeclaration();
            //获取类型
            String type = null;
            if(tmpNameDeclaration != null && tmpNameDeclaration instanceof VariableNameDeclaration){
                VariableNameDeclaration variableNameDeclaration = (VariableNameDeclaration) tmpNameDeclaration;
                if(variableNameDeclaration.getImage() != null &&  variableName.getImage() !=null){
                    if(variableName.getImage().equals(variableNameDeclaration.getImage())) {
                        type = variableNameDeclaration.getTypeImage();
                    }else{
                        String methName = variableName.getImage().split("[.]")[0];
                        Class typeClazz =variableNameDeclaration.getType();
                        if(typeClazz != null) {
                            for (Method declaredMethod : typeClazz.getDeclaredMethods()) {
                                if (declaredMethod.getName().equals(typeClazz)) {
                                    type = declaredMethod.getReturnType().getSimpleName();
                                }
                            }
                        }
                    }
                }
            } else if (tmpNameDeclaration instanceof MethodNameDeclaration) {
                ASTMethodDeclaration methodDeclaration= tmpNameDeclaration.getNode().getFirstParentOfType(ASTMethodDeclaration.class);
                type = getVariableType(methodDeclaration.getResultType());
            } else if (tmpNameDeclaration != null) {
                type = getVariableType(tmpNameDeclaration);
            } else {
                type = variableName.getImage();
            }
            //未获取到类型则不处理
            if(type !=null && TYPES.contains(type)){
                return false;
            }
        }else if(preChild instanceof  ASTAllocationExpression){
            //使用new 创建对象
            ASTAllocationExpression allocationExpression = (ASTAllocationExpression)preChild;
            ASTClassOrInterfaceType classOrInterfaceType = allocationExpression.getFirstChildOfType(ASTClassOrInterfaceType.class);
            if(classOrInterfaceType!= null){
                //获取类型
                String type = classOrInterfaceType.getImage();
                //未获取到类型则不处理
                if(type !=null && TYPES.contains(type)){
                    return false;
                }
            }
        }
        return true;
    }
    /**
     * 获取变量声明对应的数据类型
     * @param variNameDeclaration
     * @return
     */
    private String getVariableType(NameDeclaration variNameDeclaration ){
        //查询变量声明不存在，则不处理
        if(variNameDeclaration == null||variNameDeclaration.getNode()==null){
            return null;
        }
        //获取声明节点
        Node declaration= variNameDeclaration.getNode().getFirstParentOfType(ASTLocalVariableDeclaration.class);
        if(declaration == null){
            declaration = variNameDeclaration.getNode().getFirstParentOfType(ASTFieldDeclaration.class);
        }
        if(declaration ==null){
            return null;
        }
        return getVariableType(variNameDeclaration.getNode());
    }

    /**
     * 获取变量声明中的数据类型
     * @param declaration
     * @return
     */
    private String getVariableType(Node declaration){
        //查询变量声明不存在，则不处理
        if(declaration == null){
            return null;
        }
        //基本类型
        ASTPrimitiveType primitiveType  = declaration.getFirstDescendantOfType(ASTPrimitiveType.class);
        if(primitiveType == null) {
            //引用类型
            ASTReferenceType referenceType = declaration.getFirstDescendantOfType(ASTReferenceType.class);
            if(referenceType != null){
                ASTClassOrInterfaceType classOrInterfaceType = referenceType.getFirstChildOfType(ASTClassOrInterfaceType.class);
                if(classOrInterfaceType != null) {
                    return classOrInterfaceType.getImage();
                }
            }
        }else{
            return primitiveType.getImage();
        }
        return null;
    }
}

