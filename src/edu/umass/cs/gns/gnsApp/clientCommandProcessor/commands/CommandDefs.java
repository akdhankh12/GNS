/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved 
 *
 * Initial developer(s): Westy.
 */
package edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands;

/**
 * All the command classes supported by the GNS server are listed here.
 *
 * @author westy
 */
public class CommandDefs {

  private static String[] commands = new String[]{
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.Append",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendList",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendListSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendListUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendListWithDuplication",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendListWithDuplicationSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendListWithDuplicationUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendOrCreate",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendOrCreateList",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendOrCreateListSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendOrCreateListUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendWithDuplication",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendWithDuplicationSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.AppendWithDuplicationUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.Clear",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ClearSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ClearUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.Create",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.CreateEmpty",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.CreateEmptySelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.CreateList",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.CreateListSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.CreateSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.Help",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.Read",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReadMultiField",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReadArray",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReadArrayOne",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReadArrayOneSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReadArrayOneUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReadArraySelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReadArrayUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.Remove",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.RemoveField",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.RemoveFieldSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.RemoveFieldUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.RemoveList",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.RemoveListSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.RemoveListUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.RemoveSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.RemoveUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.Replace",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceList",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceListSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceListUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceOrCreate",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceOrCreateList",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceOrCreateListSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceOrCreateListUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceOrCreateSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceOrCreateUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.ReplaceUserJSON",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.Select",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SelectGroupLookupQuery",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SelectGroupSetupQuery",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SelectGroupSetupQueryWithGuid",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SelectGroupSetupQueryWithGuidAndInterval",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SelectGroupSetupQueryWithInterval",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SelectNear",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SelectWithin",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SelectQuery",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.Substitute",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SubstituteList",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SubstituteListSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SubstituteListUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SubstituteSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SubstituteUnsigned",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.Set",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SetSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SetFieldNull",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.data.SetFieldNullSelf",
    // Account
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.AddAlias",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.AddGuid",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.AddMultipleGuids",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.LookupAccountRecord",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.LookupRandomGuids", //for testing 
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.LookupGuid",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.LookupPrimaryGuid",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.LookupGuidRecord",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.RegisterAccount",
    //"edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.RegisterAccountWithoutGuid",
    //"edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.RegisterAccountWithoutGuidOrPassword",
    //"edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.RegisterAccountWithoutPassword",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.RemoveAccount",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.RemoveAlias",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.RemoveGuid",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.RemoveGuidNoAccount",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.RetrieveAliases",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.SetPassword",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.account.VerifyAccount",
    // ACL
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.acl.AclAdd",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.acl.AclAddSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.acl.AclRemove",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.acl.AclRemoveSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.acl.AclRetrieve",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.acl.AclRetrieveSelf",
    // Group
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.AddMembersToGroup",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.AddMembersToGroupSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.AddToGroup",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.AddToGroupSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.GetGroupMembers",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.GetGroupMembersSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.GetGroups",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.GetGroupsSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.RemoveFromGroup",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.RemoveFromGroupSelf",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.RemoveMembersFromGroup",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.RemoveMembersFromGroupSelf",
    //    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.GrantMembership",
    //    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.GrantMemberships",
    //    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.RequestJoinGroup",
    //    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.RequestLeaveGroup",
    //    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.RetrieveGroupJoinRequests",
    //    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.RetrieveGroupLeaveRequests",
    //    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.RevokeMembership",
    //    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.group.RevokeMemberships",
    //Admin
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.Admin",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.BatchCreateGuidSimple",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.BatchCreateGuidName",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.BatchCreateGuid",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.GetParameter",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.SetParameter",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.ListParameters",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.DeleteAllRecords",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.ResetDatabase",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.ClearCache",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.DumpCache",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.ChangeLogLevel",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.AddTag",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.RemoveTag",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.ClearTagged",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.GetTagged",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.Dump",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.PingTable",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.PingValue",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.RTT",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.RTTQuick",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.admin.ConnectionCheck",
    // Active code
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.activecode.Set",
    "eedu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.activecode.Clear",
    "edu.umass.cs.gns.gnsApp.clientCommandProcessor.commands.activecode.Get"

  };

  /**
   *
   * @return
   */
  public static String[] getCommandDefs() {
    return commands;
  }
}