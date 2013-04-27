package android.annotation;

import java.lang.annotation.Annotation;

public @interface MokeeHook
{
    public abstract MokeeHookType value();

    public static enum MokeeHookType
    {
        CHANGE_ACCESS,
        CHANGE_CODE,
        CHANGE_CODE_AND_ACCESS,
        CHANGE_PARAMETER,
        CHANGE_PARAMATER_AND_ACCESS,
        CHANGE_BASE_CLASS,
        NEW_CLASS,
        NEW_FIELD,
        NEW_METHOD;
    }
}
