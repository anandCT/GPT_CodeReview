# In this basic model we get code suggestion from the llm only. No code style used.

import os
from gpt4all import GPT4All
import sys

SYSTEM_TEMPLATE = ('You are a senior developer for reviewing the code. Use the following given context to give '
                   'suggestions on improving the code quality depending on the question asked. If you do not have any '
                   'suggestions, just say Code looks good. Keep the suggestions concise.')


def get_chunks(file_content, chunk_size=1900):
    return [file_content[i:i + chunk_size] for i in range(0, len(file_content), chunk_size)]


def read_file():
    try:
        with open("review/findPrimeNumbers.py", "r", encoding="utf-8") as file:
            file_content = file.read()
        print(f'file content: {file_content}')
        file_name = os.path.basename("review/findPrimeNumbers.py.py")
        content_chunks = get_chunks(file_content)
        get_code_suggestions(file_name, content_chunks)

    except Exception as e:
        print(e)


def get_code_suggestions(file_name, content_chunks):
    print("\nGenerating suggestions ...")

    model = GPT4All(model_name='orca-mini-3b-gguf2-q4_0.gguf', allow_download=False, model_path='models/')

    for idx, chunk in enumerate(content_chunks, 1):
        prompt = (f"Context: The file '{file_name}' (chunk '{str(idx)}') contains: {chunk}. "
                  f"Could you give some recommendations for improving the code? and sorting suggestions list by "
                  f"priority from high to low.")

        with model.chat_session(SYSTEM_TEMPLATE):
            for token in model.generate(prompt=prompt, temp=0, max_tokens=1000, streaming=True):
                sys.stdout.write(token)

    sys.stdout.flush()


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    read_file()
