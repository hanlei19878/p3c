package com.alibaba.p3c.pmd.lang.xml.rule.mybatis;

import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.xml.ast.XmlNode;
import org.w3c.dom.Element;

public class NoSelectColumnsRule extends AbstractMybatisRule {
    private static final String MESSAGE_KEY_PREFIX = "xml.mybatis.dc.NoSelectColumnsRule.rule.violation.msg";

    @Override
    public void visitSelect(XmlNode node, Element element, RuleContext ctx) {
        if(node.getNode().getTextContent().toLowerCase().split("(select){1}[\r\n\t ]*[*]{1}").length>1){
            addViolationWithMessage(ctx, node, MESSAGE_KEY_PREFIX, new String[]{element.getAttribute("id")});
        }
    }
}
