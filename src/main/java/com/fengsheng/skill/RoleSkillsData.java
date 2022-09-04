package com.fengsheng.skill;

import com.fengsheng.protos.Common;

import java.util.Arrays;

public class RoleSkillsData {
    private String name;
    private Common.role role;
    private boolean faceUp;
    private Skill[] skills;

    /**
     * 角色技能数据
     *
     * @param name   角色名
     * @param role   角色对应协议中的枚举
     * @param faceUp 角色是否面朝上
     * @param skills 角色技能
     */
    public RoleSkillsData(String name, Common.role role, boolean faceUp, Skill... skills) {
        this.name = name;
        this.role = role;
        this.faceUp = faceUp;
        this.skills = Arrays.copyOf(skills, skills.length);
    }

    public RoleSkillsData(RoleSkillsData data) {
        this(data.name, data.role, data.faceUp, data.skills);
    }

    /**
     * 获取角色名
     */
    public String getName() {
        return name;
    }

    /**
     * 获取角色对应协议中的枚举
     */
    public Common.role getRole() {
        return role;
    }

    /**
     * 获取角色是否面朝上
     */
    public boolean isFaceUp() {
        return faceUp;
    }

    /**
     * 设置角色是否面朝上
     */
    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    /**
     * 获取角色技能
     */
    public Skill[] getSkills() {
        return skills;
    }

    /**
     * 设置角色技能
     */
    public void setSkills(Skill[] skills) {
        this.skills = skills;
    }
}
