package com.koubilgi.submenus;

public class SubmenuManager
{
    public final static Submenu[] submenus = new Submenu[]{new Grades(), new Syllabus(), new Meals(), new General(),
            new ExamSchedule(), new FeeStatus(),};

    public static Submenu getSubmenuByName(int nameResource)
    {
        for (Submenu submenu : submenus)
        {
            if (submenu.getNameResource() == nameResource)
                return submenu;
        }

        return null;
    }
}
