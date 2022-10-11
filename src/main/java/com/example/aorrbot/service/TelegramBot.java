package com.example.aorrbot.service;

import com.example.aorrbot.config.BotConfiguration;
import com.example.aorrbot.model.User;
import com.example.aorrbot.repository.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private static final String HELP_TEXT = "Информационное окно: \n\n" +
            "Нажмите /start для приветствия";

    private BotConfiguration configuration;

    private UserRepository userRepository;

    @Autowired
    public TelegramBot(BotConfiguration configuration, UserRepository userRepository) {
        this.configuration = configuration;
        this.userRepository = userRepository;
        List<BotCommand> botCommand = new ArrayList<>();
        botCommand.add(new BotCommand("/start",EmojiParser.parseToUnicode("Приветствие" + ":see_no_evil:")));
        botCommand.add(new BotCommand("/mydata",EmojiParser.parseToUnicode("Узнать мои данные" + ":hear_no_evil:")));
        botCommand.add(new BotCommand("/deletedata",EmojiParser.parseToUnicode("Удалить мои данные" + ":speak_no_evil:")));
        botCommand.add(new BotCommand("/help",EmojiParser.parseToUnicode("Информация" + ":monkey_face:")));
        botCommand.add(new BotCommand("/settings",EmojiParser.parseToUnicode("Настройки" + ":banana:")));
        try {
            this.execute(new SetMyCommands(botCommand, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return configuration.getBotName();
    }

    @Override
    public String getBotToken() {
        return configuration.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String userName = update.getMessage().getChat().getFirstName();
            long chatId = update.getMessage().getChatId();
            if (messageText.contains("/send") && configuration.getAdminId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                Iterable<User> users = userRepository.findAll();
                for (User user : users) {
                    sendMessage(user.getChatId(), textToSend, null);
                }
            }
            switch (messageText) {
                case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, userName);
                    break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT,null);
                    break;
                case "/settings":
                    settings(chatId);
                    break;
                default:
                        sendMessage(chatId, "Команда не поддерживается",null);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            if (callbackData.equals("ONE_BUTTON")) {
                String text = "Первая кнопка";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatId));
                message.setText(text);
                message.setMessageId((int) messageId);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    log.error("Error occured: " + e.getMessage());
                }
            } else if (callbackData.equals("two_button")) {
                String text = "Вторая кнопка";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatId));
                message.setText(text);
                message.setMessageId((int)messageId);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    log.error("Error occured: " + e.getMessage());
                }
            }

        }
    }

    private void settings(long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("Настройки");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var buttonOne = new InlineKeyboardButton();
        buttonOne.setText("Первая настройка");
        buttonOne.setCallbackData("ONE_BUTTON");
        var buttonTwo = new InlineKeyboardButton();
        buttonTwo.setText("Вторая настройка");
        buttonTwo.setCallbackData("two_button");
        rowInLine.add(buttonOne);
        rowInLine.add(buttonTwo);
        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        msg.setReplyMarkup(markupInLine);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }

    private void registerUser(Message msg) {
        if(userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();
            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisterAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void startCommandReceived (long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Привет, " + name + ", рад видеть тебя!" + ":see_no_evil:");
        log.info("Replied to user: " + name);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Погода");
        row.add("Город");
        row.add("Страна");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        sendMessage(chatId, answer, keyboardMarkup);
    }
    private void sendMessage (long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }
}
