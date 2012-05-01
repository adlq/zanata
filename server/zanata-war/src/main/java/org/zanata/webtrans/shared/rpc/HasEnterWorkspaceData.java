package org.zanata.webtrans.shared.rpc;

import org.zanata.webtrans.shared.auth.SessionId;
import org.zanata.webtrans.shared.model.Person;

public interface HasEnterWorkspaceData
{
   Person getPerson();

   SessionId getSessionId();
}
