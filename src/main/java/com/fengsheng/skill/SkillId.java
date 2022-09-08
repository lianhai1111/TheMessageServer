package com.fengsheng.skill;

public enum SkillId {
    LIAN_LUO("联络"), MING_ER("明饵"), XIN_SI_CHAO("新思潮"), MIAN_LI_CANG_ZHEN("绵里藏针"),
    QI_HUO_KE_JU("奇货可居"), JIN_SHEN("谨慎"), GUI_ZHA("诡诈"), YI_YA_HUAN_YA("以牙还牙"),
    TOU_TIAN("偷天"), HUAN_RI("换日");

    private final String name;

    SkillId(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
