package com.alibaba.p3c.pmd.lang.java.dc.rule.doc;

import com.alibaba.p3c.pmd.I18nResources;
import com.alibaba.p3c.pmd.lang.java.rule.comment.AbstractAliCommentRule;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTConstructorDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTMethodDeclaration;
import net.sourceforge.pmd.lang.java.ast.AbstractJavaAccessNode;
import net.sourceforge.pmd.properties.EnumeratedProperty;

import java.util.Arrays;

/**
 * <p>Name: DcCommentRequiredRule</p>
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
 * Create at 2019/3/11 10:43
 * <p>
 * Copyright @ dcitc.com . All Rights Reserved.
 */
public class DcCommentRequiredRule extends AbstractAliCommentRule {
    enum CommentRequirement {
        Required("Required"), Ignored("Ignored"), Unwanted("Unwanted");

        private final String label;

        CommentRequirement(String theLabel) {
            label = theLabel;
        }

        public static String[] labels() {
            String[] labels = new String[values().length];
            int i = 0;
            for (DcCommentRequiredRule.CommentRequirement requirement : values()) {
                labels[i++] = requirement.label;
            }
            return labels;
        }
    }

    public static final String MESSAGE_METHOD = "java.doc.dc.DcCommentRequiredRule.violation.msg.methodCommentRequirement.";
    public static final EnumeratedProperty<CommentRequirement> PUB_METHOD_CMT_REQUIREMENT_DESCRIPTOR = new EnumeratedProperty<>(
            "publicMethodCommentRequirement", "Public method and constructor comments. Possible values: " + Arrays.toString(DcCommentRequiredRule.CommentRequirement.values()),
            DcCommentRequiredRule.CommentRequirement.labels(), DcCommentRequiredRule.CommentRequirement.values(), 0, 3.0f);

    public static final EnumeratedProperty<DcCommentRequiredRule.CommentRequirement> PROT_METHOD_CMT_REQUIREMENT_DESCRIPTOR = new EnumeratedProperty<>(
            "protectedMethodCommentRequirement", "Protected method constructor comments. Possible values: " + Arrays.toString(DcCommentRequiredRule.CommentRequirement.values()),
            DcCommentRequiredRule.CommentRequirement.labels(), DcCommentRequiredRule.CommentRequirement.values(), 0, 4.0f);

    public static final EnumeratedProperty<DcCommentRequiredRule.CommentRequirement> PACK_PRI_METHOD_CMT_REQUIREMENT_DESCRIPTOR = new EnumeratedProperty<>(
            "packagePrivateMethodCommentRequirement", "Package private method and constructor comments. Possible values:" + Arrays.toString(DcCommentRequiredRule.CommentRequirement.values()),
            DcCommentRequiredRule.CommentRequirement.labels(), DcCommentRequiredRule.CommentRequirement.values(), 0, 5.0f);

    public DcCommentRequiredRule() {
        definePropertyDescriptor(PUB_METHOD_CMT_REQUIREMENT_DESCRIPTOR);
        definePropertyDescriptor(PROT_METHOD_CMT_REQUIREMENT_DESCRIPTOR);
        definePropertyDescriptor(PACK_PRI_METHOD_CMT_REQUIREMENT_DESCRIPTOR);
    }

    private DcCommentRequiredRule.CommentRequirement getCommentRequirement(String label) {
        if (DcCommentRequiredRule.CommentRequirement.Ignored.label.equals(label)) {
            return DcCommentRequiredRule.CommentRequirement.Ignored;
        } else if (DcCommentRequiredRule.CommentRequirement.Required.label.equals(label)) {
            return DcCommentRequiredRule.CommentRequirement.Required;
        } else if (DcCommentRequiredRule.CommentRequirement.Unwanted.label.equals(label)) {
            return DcCommentRequiredRule.CommentRequirement.Unwanted;
        } else {
            return null;
        }
    }

    @Override
    public Object visit(ASTConstructorDeclaration decl, Object data) {
        checkComment(decl, data);
        return super.visit(decl, data);
    }

    @Override
    public Object visit(ASTMethodDeclaration decl, Object data) {
        checkComment(decl, data);
        return super.visit(decl, data);
    }


    @Override
    public Object visit(ASTCompilationUnit cUnit, Object data) {
        assignCommentsToDeclarations(cUnit);

        return super.visit(cUnit, data);
    }

    private void checkComment(AbstractJavaAccessNode decl, Object data) {
        EnumeratedProperty<DcCommentRequiredRule.CommentRequirement> commentProperty= null;
        if (decl.isPublic()) {
            commentProperty = PUB_METHOD_CMT_REQUIREMENT_DESCRIPTOR;
        }else if(decl.isProtected()){
            commentProperty = PROT_METHOD_CMT_REQUIREMENT_DESCRIPTOR;
        }else if(decl.isPackagePrivate()){
            commentProperty = PACK_PRI_METHOD_CMT_REQUIREMENT_DESCRIPTOR;
        }else{
            return;
        }
        String descriptorName = commentProperty.name();
        CommentRequirement commentRequirement = getCommentRequirement(getProperty(
                commentProperty).toString());
        if (commentRequirement != DcCommentRequiredRule.CommentRequirement.Ignored) {
            if (commentRequirement == DcCommentRequiredRule.CommentRequirement.Required) {
                if (decl.comment() == null) {
                    if(decl instanceof  ASTMethodDeclaration) {
                        addViolationWithMessage(data, decl,
                                I18nResources.getMessage(MESSAGE_METHOD + DcCommentRequiredRule.CommentRequirement.Required, ((ASTMethodDeclaration) decl).getName()),
                                decl.getBeginLine(), decl.getEndLine());
                    }else{
                        addViolationWithMessage(data, decl,
                                I18nResources.getMessage(MESSAGE_METHOD +"default."+ DcCommentRequiredRule.CommentRequirement.Required),
                                decl.getBeginLine(), decl.getEndLine());
                    }
                }
            } else {
                if (decl.comment() != null) {
//                    addViolationWithMessage(data, decl,
//                            descriptorName
//                                    + " " + DcCommentRequiredRule.CommentRequirement.Unwanted,
//                            decl.getBeginLine(), decl.getEndLine());
                }
            }
        }
    }

/*
    @Override
    public Object visit(ASTCompilationUnit cUnit, Object data) {
        assignCommentsToDeclarations(cUnit);

        return super.visit(cUnit, data);
    }
*/

    public boolean allCommentsAreIgnored() {

        return getProperty(PUB_METHOD_CMT_REQUIREMENT_DESCRIPTOR) == DcCommentRequiredRule.CommentRequirement.Ignored
                && getProperty(PROT_METHOD_CMT_REQUIREMENT_DESCRIPTOR) == DcCommentRequiredRule.CommentRequirement.Ignored
                && getProperty(PACK_PRI_METHOD_CMT_REQUIREMENT_DESCRIPTOR) == DcCommentRequiredRule.CommentRequirement.Ignored;
    }


    @Override
    public String dysfunctionReason() {
        return allCommentsAreIgnored() ? "All comment types are ignored" : null;
    }
}
