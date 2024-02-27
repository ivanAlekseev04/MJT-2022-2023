package uni.sofia.fmi.mjt.project.commands;

public abstract class InputRules {
    public static final String HELP_INFO = """
            (!) Syntax for non-existed(null) parameter: <delete-task #name:Test> (#date is null there)
                        
            -> help-task {additional info about commands to operate with tasks through account}
            -> help-collaboration {additional info about commands to operate with collaborations}
            
            -> register #name:<text> #password:<text> {example: register #name:Stoyo #password:SAP chief}
            -> login #name:<text> #password:<text> {log in system with the same data, that was used while registration}
            -> logout {logging out from system by current user. 
            Then client can register new profile or log in to already existed}
            """;
    public static final String HELP_COLLABORATION_INFO = """
            |General|
            (!) opt -> optional = parameter can be null 
            (!) To perform any of below actions, user need to be logged in
            
            |Add collaboration|
            -> add-collaboration #name:<text>
            
            |List collaborations|
            -> list-collaborations (list names of all collaborations to that concrete user is belong to)
            
            (!) To perform any of below actions, user need to be member of concrete collaboration
            
            |Delete collaboration|
            (!) One collaboration can be deleted only by its owner (User, that created it)
            
            -> delete-collaboration #collaboration:<text> (delete collaboration with stated name and all tasks from it)
            
            |Add user|
            (!) New user to collaboration can be added only by its member
            
            -> add-user #collaboration:<text> #name:<text>
            
            |Add task|
            (1) WITH optional parameter "#assignee" stated: task will be seen only by specified user
            (2) WITHOUT optional parameter "#assignee" stated: task will be seen by all members of stated collaboration
            
            -> collaboration-add-task #collaboration:<text> #assignee:<user-name> #name:<task name>
             (opt)#date:<YYYY-MM-DD> (opt)#due-date:<YYYY-MM-DD> (opt)#description:<text>
            
            |List tasks|
            (!) Only members of stated collaboration can get all tasks info from it
            (!) "Assigned tasks" can be seen only by their assignee
            
            -> collaboration-list-tasks #collaboration:<text> (give info about all tasks from specified collaboration)
            
            |List users|
            (!) Only members of stated collaboration can get info of all other members, linked to it
             
            -> list-users #collaboration:<text> (give names of all users -> members of specified collaboration)
            """;
    public static final String HELP_TASK_INFO = """
            |General|
            (!) opt -> optional = parameter can be null
            (!) when parameter "#cur-date:" is null (non-stated at all) 
            -> this task don't have exact date (is in inbox)
            (!) when parameter "#cur-date:" is not null (stated with right format) 
            -> this task need to be done exactly on this day
            (!) When both "#date" and "#due-date" are stated: "#date"
             need to be before or equal to "#due-date" 
                        
            |Adding task|
            (!) Task can be added only when user is logged in
            
            -> add-task #name:<text> (opt)#date:<YYYY-MM-DD> (opt)#due-date:<YYYY-MM-DD> (opt)#description:<text>
                        
            |Updating task|
            (!) Parameter "#name:" can't be changed
            (Limitation!) If task was added with "#date" parameter, then it can't be moved to inbox.
            Only changes to another valid date are available.
                         
            -> update-task #name:<text> (opt)#cur-date:<YYYY-MM-DD> (opt)#date:<YYYY-MM-DD>
            (opt)#due-date:<YYYY-MM-DD> (opt)#description:<text>
            
            |Delete task|
            (!) Without stating "#date" parameter task will be considered as inbox
            
            -> delete-task #name:<text> (opt)#date:<YYYY-MM-DD>
            
            |Get task|
            (!) Without stating "#date" parameter task will be considered as inbox
            
            -> get-task #name:<text> (opt)#date:<YYYY-MM-DD>
            
            |List tasks|
            (!) This command has 2 variations: with "#date<YYYY-MM-DD>" and with "#completed"
            
            -> list-tasks (give info about all tasks from inbox)
            -> list-tasks #date:<YYYY-MM-DD> (give about all tasks from stated date)
            -> list-tasks #completed (give info about all already completed tasks)
            
            |Finish task|
            (!) Without stating "#date" parameter task will be considered as inbox
            
            -> finish-task #name:<text> (opt)#date:<YYYY-MM-DD>
            
            |List dashboard|
            (!) Can't have parameters at all
            
            -> list-dashboard
            """;
}
