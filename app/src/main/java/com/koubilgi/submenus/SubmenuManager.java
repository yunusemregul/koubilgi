package com.koubilgi.submenus;

public class SubmenuManager
{
    public final static Submenu[] submenus = new Submenu[]{
            new GradesSubmenu(),
            new SyllabusSubmenu(),
            new MealsSubmenu(),
            new GeneralSubmenu(),
            new ExamScheduleSubmenu(),
            new FeeStatusSubmenu(),
    };
}
