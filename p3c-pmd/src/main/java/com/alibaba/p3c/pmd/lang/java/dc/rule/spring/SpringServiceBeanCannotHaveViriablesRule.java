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

import com.alibaba.p3c.pmd.I18nResources;
import com.alibaba.p3c.pmd.lang.java.rule.AbstractAliRule;
import com.alibaba.p3c.pmd.lang.java.util.ViolationUtils;
import com.alibaba.p3c.pmd.lang.java.util.namelist.NameListConfig;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.*;
import net.sourceforge.pmd.lang.java.symboltable.VariableNameDeclaration;
import net.sourceforge.pmd.lang.symboltable.NameOccurrence;
import org.jaxen.JaxenException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * [Mandatory] Make sure to invoke the rollback if a method throws an Exception.
 *
 * @author caikang
 * @date 2017/03/29
 */
public class SpringServiceBeanCannotHaveViriablesRule extends AbstractAliRule {
    private static final Set<String> SPRING_SERVICE_ANNOTATION_NAMES = new HashSet<>(NameListConfig.NAME_LIST_SERVICE.getNameList(
            "SpringServiceBeanCannotHaveViriablesRule", "ANNOTATION_NAME_LIST"));

    @Override
    public Object visit(ASTFieldDeclaration node, Object data) {
        //不可变变量不用处理,对于标识为静态的也不处理
        if(node.isFinal()||node.isStatic()){
            return super.visit(node, data);
        }
        //获取属性对应的类
        // 后续如出现问题，切换至字段属性判断上
        return super.visit(node, data);
    }
    @Override
    public Object visit(ASTAnnotation node, Object data) {
        ASTName name = node.getFirstDescendantOfType(ASTName.class);
        //识别注解
        if(name == null ||!SPRING_SERVICE_ANNOTATION_NAMES.contains(name.getImage())){
            return super.visit(node, data);
        }
        //获取类
        ASTClassOrInterfaceDeclaration classOrInterfaceDeclaration
                = getSiblingForType(node, ASTClassOrInterfaceDeclaration.class);
        //查看实现接口，5.30版本指标实现了接口IProductPart，此类不做处理
        List<ASTClassOrInterfaceType> implementsLists= null;
        ASTImplementsList implementsList =classOrInterfaceDeclaration.getFirstChildOfType(ASTImplementsList.class);
        if(implementsList == null){
            implementsLists = null;
        }else{
            implementsLists = implementsList.findChildrenOfType(ASTClassOrInterfaceType.class);
        }
        boolean springAppClass = false;
        if(implementsLists  != null){
            for (ASTClassOrInterfaceType implement : implementsLists) {
                if(implement != null && implement.getImage()!=null &&
                        ("IProductPart".equals(implement.getImage())
                                || "com.dcits.ensemble.product.api.application.IProductPart".equals(implement.getImage()))){
                    return super.visit(node, data);
                }

                if(implement != null && implement.getImage()!=null &&
                        (   "ApplicationContextAware".equals(implement.getImage())
                                || "org.springframework.context.ApplicationContextAware".equals(implement.getImage())
                                ||"ApplicationListener".equals(implement.getImage())
                                || "org.springframework.context.ApplicationListener".equals(implement.getImage())
                        )){
                    springAppClass = true;
                }
            }
        }
        //获取属性

        List<ASTFieldDeclaration> fieldDeclarationList = null;
        Map<VariableNameDeclaration, List<NameOccurrence>> variables = classOrInterfaceDeclaration.getScope().getDeclarations(VariableNameDeclaration.class);

        for (Map.Entry<VariableNameDeclaration, List<NameOccurrence>> variableEntry : variables.entrySet()) {
            VariableNameDeclaration vir = variableEntry.getKey();
            AccessNode accessNode =  vir.getAccessNodeParent();
            //不可变变量不用处理
            if(accessNode.isFinal()){
                continue;
            }
            //Logger属性变量不处理
            try {
                String typeName = accessNode.getFirstChildOfType(ASTType.class).getFirstChildOfType(ASTReferenceType.class).getFirstChildOfType(ASTClassOrInterfaceType.class).getImage();
                if("Logger".equals(typeName)){
                    continue;
                }
            }catch (Exception e){
//                e.printStackTrace();
            }
            //对于标识为静态的也不处理
//            if(accessNode.isStatic()){
//                continue;
//            }
            //识别自动注入注解
            boolean findAnnotation = false;
            List<ASTAnnotation> annotations
                    = accessNode.jjtGetParent().findChildrenOfType(ASTAnnotation.class);
            if(annotations != null){
                for (ASTAnnotation annotation : annotations) {
//                    ASTMarkerAnnotation markerAnnotation = annotation.getFirstChildOfType(ASTMarkerAnnotation.class);
//                    if(markerAnnotation == null) {
//                        continue;
//                    }
                    ASTName astName = annotation.getFirstDescendantOfType(ASTName.class) ;//markerAnnotation.getFirstDescendantOfType(ASTName.class);
                    if(astName == null&&astName.getImage()==null){
                        continue;
                    }
                    if("Resource".equals(astName.getImage()) || "Autowired".equals(astName.getImage())){
                        findAnnotation = true;
                    }
                    //系统启动加载工厂类处理
                    if(springAppClass){
                        if("SuppressWarnings".equals(astName.getImage())){
                            try {
                                String annotaName =annotation.getFirstDescendantOfType(ASTMemberValue.class).getFirstDescendantOfType(ASTLiteral.class).getImage();
                                if (annotaName.endsWith("SpringServiceBeanCannotHaveViriables\"")) {
                                    findAnnotation = true;
                                }
                            }catch (Exception e){};
                        }
                    }
                }
            }
            //增加错误信息
            if(!findAnnotation){
                ViolationUtils.addViolationWithPrecisePosition(this, accessNode, data,
                        I18nResources.getMessage("java.spring.dc.SpringServiceBeanCannotHaveViriablesRule.violation.msg",
                                accessNode.getImage()));
            }

        }


        return super.visit(node, data);
    }
   /**
     * annotation is sibling of classOrInterface declaration or method declaration
     *
     * @param node transactional annotation
     * @param clz  classOrInterface declaration or method declaration
     * @param <T>  generic
     * @return sibling node
     */
    private <T> T getSiblingForType(ASTAnnotation node, Class<T> clz) {
        Node parent = node.jjtGetParent();
        int num = parent.jjtGetNumChildren();
        for (int i = 0; i < num; i++) {
            Node child = parent.jjtGetChild(i);
            if (clz.isAssignableFrom(child.getClass())) {
                return clz.cast(child);
            }
            if (!(child instanceof ASTAnnotation)) {
                return null;
            }
        }
        return null;
    }
}
