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
package com.alibaba.p3c.pmd.lang.java.dc.rule.naming;

import com.alibaba.p3c.pmd.I18nResources;
import com.alibaba.p3c.pmd.lang.AbstractXpathRule;
import com.alibaba.p3c.pmd.lang.java.util.ViolationUtils;
import com.alibaba.p3c.pmd.lang.java.util.namelist.NameListConfig;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;

import java.util.HashSet;
import java.util.Set;

/**
 * [Mandatory] Do not add 'is' as prefix while defining Boolean variable, since it may cause a serialization exception
 * in some Java Frameworks.
 *
 * @author changle.lq
 * @date 2017/04/16
 */
public class GalaxyServiceMethodNameNotOverrideRule extends AbstractXpathRule {
    private static final String XPATH = "//MethodDeclaration["+
            "//ExtendsList/ClassOrInterfaceType[" +
            "( @Image='AbstractService' " +
            "or @Image='AbstractProcess'" +
            "or @Image='BaseService') and " +
            "//ImportDeclaration[@ImportedName='com.dcits.ensemble.business.AbstractService'" +
            " or @ImportedName='com.dcits.orion.core.support.AbstractProcess'" +
            " or @ImportedName='com.dcits.ensemble.service.BaseService']]" +
            "and ../Annotation/MarkerAnnotation/Name[@Image='Override']" +
            "]" ;
    private static final Set<String> BLOCK_LIST = new HashSet<>(NameListConfig.NAME_LIST_SERVICE.getNameList(
            "GalaxyServiceMethodNameNotOverrideRule", "BLOCK_LIST"));

    public GalaxyServiceMethodNameNotOverrideRule() {
        setXPath(XPATH);
    }

    @Override
    public void addViolation(Object data, Node node, String arg) {
        if(node instanceof ASTMethodDeclaration){
            if(BLOCK_LIST.contains(((ASTMethodDeclaration) node).getMethodName())){
                //类声明
                Node classDec = node.getNthParent(3);
                if(classDec.getImage() != null &&
                        (classDec.getImage().equals("AbstractService")||classDec.getImage().equals("BaseService"))){
                    String methName =((ASTMethodDeclaration) node).getMethodName();
                    if("beforeProcess".equals(methName)
                            ||"afterProcess".equals(methName)
                            ||"clearMDC".equals(methName)){
                        return;
                    }
                }
                ViolationUtils.addViolationWithPrecisePosition(this, node, data,
                        I18nResources.getMessage("java.naming.dc.GalaxyServiceMethodNameNotOverrideRule.violation.msg",
                                node.getImage()));
            }
        }
    }
}
