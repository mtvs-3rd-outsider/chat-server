package com.outsider.mop

import com.outsider.mop.chat.repository.Message
import com.outsider.mop.chat.service.MessageVM
import java.time.temporal.ChronoUnit.MINUTES

fun MessageVM.prepareForTesting() = copy(id = null, sent = sent.truncatedTo(MINUTES))

fun Message.prepareForTesting() = copy(id = null, sent = sent.truncatedTo(MINUTES))