package com.alibaba.p3c.pmd.lang.xml.rule.mybatis;

import com.alibaba.p3c.pmd.lang.xml.rule.AbstractDcXmlRule;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.xml.ast.XmlNode;
import net.sourceforge.pmd.lang.xml.ast.XmlParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

public abstract class AbstractMybatisRule extends AbstractDcXmlRule {
    @Override
    public void apply(List<? extends Node> nodes, RuleContext ctx) {
        if(!nodes.isEmpty()) {
            try {
                if (((Document) ((XmlParser.RootXmlNode) nodes.get(0)).getNode())
                        .getDoctype().getSystemId().contains("mybatis-3-mapper.dtd")) {
                    super.apply(nodes, ctx);
                }
            }catch (Exception e){}
        }
    }


    @Override
    protected void visit(XmlNode node, Element element, RuleContext ctx) {
        if(node.getNode().getNodeName().equalsIgnoreCase("select")){
            visitSelect(node,element,ctx);
        }else if(node.getNode().getNodeName().equalsIgnoreCase("delete")){
            visitDelete(node,element,ctx);
        }else if(node.getNode().getNodeName().equalsIgnoreCase("update")){
            visitUpdate(node,element,ctx);
        }else if(node.getNode().getNodeName().equalsIgnoreCase("insert")){
            visitInsert(node,element,ctx);
        }else {
            super.visit(node, element, ctx);
        }
    }

    public void visitSelect(XmlNode node, Element element, RuleContext ctx)
    {
        super.visit(node, element, ctx);
    }

    public void visitDelete(XmlNode node, Element element, RuleContext ctx) {
        super.visit(node, element, ctx);
    }

    public void visitUpdate(XmlNode node, Element element, RuleContext ctx) {
        super.visit(node, element, ctx);
    }

    public void visitInsert(XmlNode node, Element element, RuleContext ctx) {
        super.visit(node, element, ctx);
    }
}
