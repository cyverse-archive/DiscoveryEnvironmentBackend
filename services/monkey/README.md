# monkey

This is a service that synchronizes the `tag` documents in the `data` search index with the 
`metadata` database.

## Requirements/Design/Implementation Notes

1. It should use the same exchange as infosquito to make it easier for scripts to trigger both at once.

1. It should first read all of the current `tag` documents and make sure that they are still in the `metadata` database.
   Both the tag needs to be present and all of the files and folders it is attached to need to be present, and no more.
   
1. Second, it should reindex everything in the `tags` table making sure all of the entries in the `attached_tags` table
   are present.
