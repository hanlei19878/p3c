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
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;

/**
 * [Mandatory] Class names should be nouns in UpperCamelCase except domain models: DO, BO, DTO, VO, etc.
 *
 * @author hanlei
 * @date 2019/03/06
 */
public class DaoImplShouldNotImplBaseDaoRule extends AbstractXpathRule {

    private static final String XPATH = "//ExtendsList/ClassOrInterfaceType[@Image='BaseDao'" +
            " and (//ImportDeclaration/Name[@Image='com.dcits.orion.core.dao.BaseDao'])]";

    public DaoImplShouldNotImplBaseDaoRule(){
        super.setXPath(XPATH);
    }

    @Override
    public void addViolation(Object data, Node node, String arg) {
        ASTClassOrInterfaceDeclaration classOrInterfaceDeclaration = node.getFirstParentOfType(ASTClassOrInterfaceDeclaration.class);
        if(classOrInterfaceDeclaration == null
                ||!classOrInterfaceDeclaration.isAbstract()){
            ViolationUtils.addViolationWithPrecisePosition(this, node, data,
                    I18nResources.getMessage("java.naming.dc.DaoImplShouldNotImplBaseDaoRule.violation.msg",
                            node.getImage()));
        }
    }
}
