import os

from telegram.ext import Updater

token = os.environ['token']
chat_id = "-154311545"
updater = Updater(token=token)
bot = updater.bot
artifacts_dir = os.environ['CIRCLE_ARTIFACTS']
filename = "AndroidMusicBot.apk"
file_path = os.path.join(artifacts_dir, filename)
if os.path.isfile(file_path):
    with open(file_path, 'rb') as bot_file:
        bot.send_document(chat_id=chat_id, document=bot_file, filename=filename, disable_notification=True)
updater.stop()
