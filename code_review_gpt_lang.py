from langchain.chains import LLMChain
from langchain.llms import GPT4All
from langchain.prompts.prompt import PromptTemplate
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_community.document_loaders import PyPDFLoader
from langchain_community.embeddings import SentenceTransformerEmbeddings
from langchain_community.llms import GPT4All
from langchain_community.vectorstores import Chroma

# This is a detailed prompt, that I have planned to use once we have implemented fetching PR changes from git sha.
SYSTEM_TEMPLATE = ('You are an expert senior developer, you have to review the pull requests created according to the '
                   '{question}'

                   'You will be given {code}. It will contain list filename and respective code but note that code '
                   'might be partial content of the file'

                   'Begin your review by evaluating the changed code against the code style provided at {context}'

                   'In your feedback, focus on highlighting if the code is following the code style guidelines or '
                   'not. If the code is not following the style guideline then you should mention it. Also, '
                   'you should highlight potential bugs, improving readability if it is a problem, making code '
                   'cleaner, and maximising the performance of the programming language.'

                   'Flag any security risks, like API keys or secrets are present in the code in plain text.'

                   'Do not suggest on breaking functions down into smaller, more manageable functions unless it is a '
                   'huge problem. Also be aware that there will be libraries and techniques used which you are not '
                   'familiar with, so do not comment on those unless you are confident that there is a problem.'

                   'Ensure the feedback details are brief, concise, accurate. If there are multiple similar issues, '
                   'only comment on the most critical.'

                   'Include brief example code snippets in the feedback details for your suggested changes when you '
                   'are confident your suggestions are improvements. Use the same programming language as the file '
                   'under review.'

                   'If there are multiple improvements you suggest in the feedback details, use an ordered list to '
                   'indicate the priority of the changes.')

guideline_text_dir = "./db/guideline_text_db"
code_text_dir = "./db/code_text_db"


def read_code_style_guideline():
    #  Read the PDF doc from the rules
    loader = PyPDFLoader("rules/BLICK-AndroidCodeStyleGuidelines.pdf")

    # Since the LLM prompt will be having limitations on length.
    # We have to recursively split the document content.
    text_splitter = RecursiveCharacterTextSplitter(
        # Set a minimal chunk size, just to show.
        chunk_size=100,
        chunk_overlap=20,
        length_function=len,
        is_separator_regex=False,
    )

    # create the open-source embedding function
    embedding_function = SentenceTransformerEmbeddings(model_name="all-MiniLM-L6-v2")

    guideline_text = ""
    for page in loader.load_and_split(text_splitter=text_splitter):
        guideline_text += page.page_content + "\n"

    guideline_split_text = text_splitter.create_documents([guideline_text])

    # load it into Chroma DB
    guideline_text_db = Chroma.from_documents(documents=guideline_split_text,
                                              embedding=embedding_function,
                                              persist_directory=guideline_text_dir)

    guideline_text_db.persist()

    guideline_vectorstore = Chroma(
        persist_directory=guideline_text_dir,
        embedding_function=embedding_function
    )

    guideline_retriever = guideline_vectorstore.as_retriever(search_kwargs={"k": 1})

    # This will be replaced with file fetch model from git sha of commits.
    with open("review/AddRecipientHelper.kt", "r", encoding="utf-8") as file:
        file_content = file.read()

    code_split_text = text_splitter.create_documents([file_content])

    code_text_db = Chroma.from_documents(documents=code_split_text,
                                         embedding=embedding_function,
                                         persist_directory=code_text_dir)

    code_text_db.persist()

    code_vectorstore = Chroma(
        persist_directory=code_text_dir,
        embedding_function=embedding_function
    )

    code_retriever = code_vectorstore.as_retriever(search_kwargs={"k": 1})

    # Create GPT model from local
    llm = GPT4All(
        model="models/orca-mini-3b-gguf2-q4_0.gguf",
        backend="llama",
    )

    # question will be part of chain invoke, which will contain code also. The code can also be a individual input
    # variable.
    # Context will be code style provided.
    prompt_template = """You are a Senior Android Developer for reviewing the pull request. Your will be given code 
    style guidelines in the context. 
    
    Your tasks will be to check if the given code is following all the style guidelines provided.
    
    If code is not following the guidelines, give suggestion for code changes. Also highlight the code guideline rule 
    for the suggestion. Using the guideline and the Android language knowledge provide some examples for the 
    suggestions. If code is following the guideline, just say the code is following the coding style guidelines.
    
    In the last give suggestions on improving the code quality and for improving the performance. List these suggestions 
    in priority highest to lowest. 
    Keep each suggestions very concise.
    
    If you do not have any suggestions, just say Code looks good. Keep the suggestions concise. 
    \nQuestion: {question} 
    \nContext: {context} 
    \nAnswer:"""

    model_prompt = PromptTemplate(template=prompt_template, input_variables=["question", "context"])

    llm_chain = LLMChain(prompt=model_prompt, llm=llm, verbose=True)
    suggestions = llm_chain.invoke(
        {
            'question': f"Give suggestions for improving the code {code_retriever} according to the coding style "
                        f"guidelines?",
            'context': guideline_retriever})
    print(f"Suggestions are: \n {suggestions}")


def format_docs(docs):
    return "\n\n".join(doc.page_content for doc in docs)


if __name__ == '__main__':
    read_code_style_guideline()
