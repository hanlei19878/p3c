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
import com.alibaba.p3c.pmd.lang.java.rule.AbstractAliRule;
import com.alibaba.p3c.pmd.lang.java.util.ViolationUtils;
import com.alibaba.p3c.pmd.lang.java.util.namelist.NameListConfig;
import net.sourceforge.pmd.lang.java.ast.ASTClassOrInterfaceDeclaration;

import java.util.List;
import java.util.regex.Pattern;

/**
 * [Mandatory] Class names should be nouns in UpperCamelCase except domain models: DO, BO, DTO, VO, etc.
 *
 * @author hanlei
 * @date 2019/03/06
 */
public class InterfaceShouldBeCamelAndPrefixRule extends AbstractAliRule {

    private static final Pattern PATTERN
        = Pattern.compile("^I{1}([A-Z][a-z0-9]+)+(([A-Z])|(DO|DTO|VO|DAO|BO|DAOImpl|YunOS|AO|PO))?$");

    @Override
    public Object visit(ASTClassOrInterfaceDeclaration node, Object data) {
        if(!node.isInterface()){
            return super.visit(node, data);
        }
        if (PATTERN.matcher(node.getImage()).matches()) {
            return super.visit(node, data);
        }
        ViolationUtils.addViolationWithPrecisePosition(this, node, data,
            I18nResources.getMessage("java.naming.InterfaceShouldBeCamelAndPrefixRule.violation.msg",
                node.getImage()));

        return super.visit(node, data);
    }
}
